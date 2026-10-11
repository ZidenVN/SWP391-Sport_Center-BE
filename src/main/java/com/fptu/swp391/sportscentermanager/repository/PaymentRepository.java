package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
