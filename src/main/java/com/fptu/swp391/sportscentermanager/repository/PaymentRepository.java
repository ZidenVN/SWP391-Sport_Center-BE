package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
            JOIN FETCH s.member
            LEFT JOIN FETCH s.membershipPackage
            LEFT JOIN FETCH p.confirmedBy
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
            LEFT JOIN FETCH p.confirmedBy
            WHERE p.paymentId = :paymentId
            """)
    Optional<Payment> findDetailById(@Param("paymentId") Long paymentId);
    // Lấy giao dịch gần nhất của một gói đăng ký
    Optional<Payment> findFirstBySubscription_SubscriptionIdOrderByPaymentDateDesc(Long subscriptionId);

    @Query("""
        select coalesce(sum(p.amount), 0) from Payment p
        where p.status in ('COMPLETED', 'SUCCESS') and coalesce(p.paidAt, p.paymentDate) between :start and :end
    """)
    BigDecimal sumReveneBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        select count(p) from Payment p
        where p.status in ('COMPLETED', 'SUCCESS') and coalesce(p.paidAt, p.paymentDate) between :start and :end
    """)
    long cntSuccessfulPaymentsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT DATE_FORMAT(COALESCE(paid_at, payment_date), '%Y-%m-%d') AS dayLabel,
               COALESCE(SUM(amount), 0) AS dailyRevenue
        FROM payments
        WHERE status IN ('COMPLETED', 'SUCCESS')
          AND COALESCE(paid_at, payment_date) BETWEEN :start AND :end
        GROUP BY DATE_FORMAT(COALESCE(paid_at, payment_date), '%Y-%m-%d')
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
          AND COALESCE(p.paidAt, p.paymentDate) BETWEEN :start AND :end
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
          AND COALESCE(p.paidAt, p.paymentDate) BETWEEN :start AND :end
        GROUP BY p.paymentMethod
    """)
    List<Object[]> getRevenueByPaymentMethod(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    // BE-18/24: khóa dòng giao dịch khi xác nhận / hủy / hoàn tiền để 2 người bấm cùng lúc không xử lý 2 lần
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentId = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    // BE-23: danh sách giao dịch toàn trung tâm. Tham số null = bỏ qua filter đó.
    @Query(value = """
        SELECT p FROM Payment p
        JOIN FETCH p.subscription s
        JOIN FETCH s.member m
        LEFT JOIN FETCH s.membershipPackage
        LEFT JOIN FETCH p.confirmedBy
        WHERE (:status IS NULL OR p.status = :status)
          AND (:method IS NULL OR p.paymentMethod = :method)
          AND (:from IS NULL OR COALESCE(p.paidAt, p.paymentDate) >= :from)
          AND (:to IS NULL OR COALESCE(p.paidAt, p.paymentDate) <= :to)
          AND (:kw IS NULL
               OR LOWER(m.email) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR m.phone LIKE CONCAT('%', :kw, '%')
               OR LOWER(CONCAT(m.lastName, ' ', m.firstName)) LIKE LOWER(CONCAT('%', :kw, '%')))
        ORDER BY p.paymentDate DESC
        """,
        countQuery = """
        SELECT COUNT(p) FROM Payment p
        JOIN p.subscription s
        JOIN s.member m
        WHERE (:status IS NULL OR p.status = :status)
          AND (:method IS NULL OR p.paymentMethod = :method)
          AND (:from IS NULL OR COALESCE(p.paidAt, p.paymentDate) >= :from)
          AND (:to IS NULL OR COALESCE(p.paidAt, p.paymentDate) <= :to)
          AND (:kw IS NULL
               OR LOWER(m.email) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR m.phone LIKE CONCAT('%', :kw, '%')
               OR LOWER(CONCAT(m.lastName, ' ', m.firstName)) LIKE LOWER(CONCAT('%', :kw, '%')))
        """)
    Page<Payment> search(@Param("status") String status,
                         @Param("method") String method,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         @Param("kw") String keyword,
                         Pageable pageable);

    // BE-24: bản không phân trang để xuất CSV
    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.subscription s
        JOIN FETCH s.member m
        LEFT JOIN FETCH s.membershipPackage
        LEFT JOIN FETCH p.confirmedBy
        WHERE (:status IS NULL OR p.status = :status)
          AND (:method IS NULL OR p.paymentMethod = :method)
          AND (:from IS NULL OR COALESCE(p.paidAt, p.paymentDate) >= :from)
          AND (:to IS NULL OR COALESCE(p.paidAt, p.paymentDate) <= :to)
          AND (:kw IS NULL
               OR LOWER(m.email) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR m.phone LIKE CONCAT('%', :kw, '%')
               OR LOWER(CONCAT(m.lastName, ' ', m.firstName)) LIKE LOWER(CONCAT('%', :kw, '%')))
        ORDER BY p.paymentDate DESC
        """)
    List<Payment> searchAll(@Param("status") String status,
                            @Param("method") String method,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            @Param("kw") String keyword);
}
