package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // Lịch sử đăng ký, mới nhất trước
    List<Subscription> findByMember_UserIdOrderByEndDateDesc(Long memberId);

    // Gói có ngày hết hạn xa nhất (dùng để nối tiếp khi gia hạn)
    Optional<Subscription> findFirstByMember_UserIdAndStatusOrderByEndDateDesc(Long memberId, String status);

    // Gói đang còn hiệu lực tại thời điểm "now"
    @Query("""
            SELECT s FROM Subscription s
            WHERE s.member.userId = :memberId
              AND s.status = 'ACTIVE'
              AND s.startDate <= :now
              AND s.endDate > :now
            ORDER BY s.endDate DESC
            """)
    List<Subscription> findCurrent(@Param("memberId") Long memberId,
                                   @Param("now") LocalDateTime now);

}
