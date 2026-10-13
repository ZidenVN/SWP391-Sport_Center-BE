package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Lịch sử thanh toán của 1 member, mới nhất trước.
    // LEFT JOIN FETCH gói tập: gói đã ngừng bán (INACTIVE) vẫn không làm mất giao dịch.
    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.subscription s
            LEFT JOIN FETCH s.membershipPackage
            WHERE s.member.userId = :memberId
            ORDER BY p.paymentDate DESC
            """)
    List<Payment> findByMemberId(@Param("memberId") Long memberId);

    // Chi tiết 1 giao dịch kèm member + gói (dùng để dựng hóa đơn)
    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.subscription s
            JOIN FETCH s.member
            LEFT JOIN FETCH s.membershipPackage
            WHERE p.paymentId = :paymentId
            """)
    Optional<Payment> findDetailById(@Param("paymentId") Long paymentId);
    // Lấy giao dịch gần nhất của một gói đăng ký
    Optional<Payment> findFirstBySubscription_SubscriptionIdOrderByPaymentDateDesc(Long subscriptionId);

    @Query("""
        select coalesce(sum(p.amount), 0) from Payment p
        where p.status in ('COMPLETED', 'SUCCESS') and p.paymentDate between :start and :end
    """)
    BigDecimal sumReveneBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        select count(p) from Payment p
        where p.status in ('COMPLETED', 'SUCCESS') and p.paymentDate between :start and :end
    """)
    long cntSuccessfulPaymentsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT DATE_FORMAT(payment_date, '%Y-%m-%d') AS dayLabel,
               COALESCE(SUM(amount), 0) AS dailyRevenue
        FROM payments
        WHERE status IN ('COMPLETED', 'SUCCESS')
          AND payment_date BETWEEN :start AND :end
        GROUP BY DATE_FORMAT(payment_date, '%Y-%m-%d')
        ORDER BY dayLabel ASC
    """, nativeQuery = true)
    List<Object[]> getRevenueByDay(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("""
        SELECT pkg.packageName,
               COALESCE(SUM(p.amount), 0),
               COUNT(p.paymentId)
        FROM Payment p
        JOIN p.subscription sub
        JOIN sub.membershipPackage pkg
        WHERE p.status in ('COMPLETED', 'SUCCESS')
          AND p.paymentDate BETWEEN :start AND :end
        GROUP BY pkg.packageName
    """)
    List<Object[]> getRevenueByPackage(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Query("""
        SELECT p.paymentMethod,
               COALESCE(SUM(p.amount), 0),
               COUNT(p.paymentId)
        FROM Payment p
        WHERE p.status in ('COMPLETED', 'SUCCESS')
          AND p.paymentDate BETWEEN :start AND :end
        GROUP BY p.paymentMethod
    """)
    List<Object[]> getRevenueByPaymentMethod(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);
}
