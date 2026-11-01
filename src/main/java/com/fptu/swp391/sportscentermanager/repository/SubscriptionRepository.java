package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

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

    List<Subscription> findByMember_UserIdAndStatus(Long memberId, String status);

    @Query("""
        SELECT COUNT(s) > 0 FROM Subscription s
        WHERE s.member.userId = :memberId
        AND s.status = 'ACTIVE'
        AND s.startDate <= :time
        AND s.endDate > :time
        """)
    boolean existsActiveCovering(@Param("memberId") Long memberId, @Param("time") LocalDateTime time);

    // BR-SUB-07: mọi gói sắp hết hạn chưa nhắc; gói đã gia hạn được lọc ở SubscriptionService.hasRenewal
    @Query("""
         select s from Subscription s
         join fetch s.member join fetch s.membershipPackage
         where s.status = 'ACTIVE'
        and s.expiryReminderSentAt is null
        and s.endDate > :now
        and s.endDate <= :deadline
        """)
    List<Subscription> findExpiringWithoutReminder(@Param("now") LocalDateTime now, @Param("deadline") LocalDateTime deadline);

    // ===== Gói tập theo buổi =====

    // Khoá dòng gói trước khi trừ / hoàn buổi, tránh 2 request cùng sửa usedSessions
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.subscriptionId = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);

    // BR-REG-02 + BR-REG-04: id các gói hợp lệ để đăng ký lớp, gói hết hạn sớm nhất đứng đầu
    @Query("""
        SELECT s.subscriptionId FROM Subscription s
        JOIN s.membershipPackage p
        JOIN p.subjects sub
        WHERE s.member.userId = :memberId
          AND s.status = 'ACTIVE'
          AND p.packageType = com.fptu.swp391.sportscentermanager.enums.PackageType.CLASS
          AND sub.subjectId = :subjectId
          AND s.startDate <= :time
          AND s.endDate > :time
          AND (p.sessionLimit IS NULL OR s.usedSessions < p.sessionLimit)
        ORDER BY s.endDate ASC
        """)
    List<Long> findValidIdsForClass(@Param("memberId") Long memberId,
                                    @Param("subjectId") Long subjectId,
                                    @Param("time") LocalDateTime time);

    // Gói ACTIVE còn hiệu lực vào thời điểm "time" (để báo đúng lý do khi không đăng ký được)
    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.membershipPackage
        WHERE s.member.userId = :memberId
          AND s.status = 'ACTIVE'
          AND s.startDate <= :time
          AND s.endDate > :time
        """)
    List<Subscription> findCovering(@Param("memberId") Long memberId, @Param("time") LocalDateTime time);

    // Gói ACTIVE chưa hết hạn, kể cả gói đã trả tiền nhưng chưa tới ngày bắt đầu (BR-SUB-05, BR-SUB-08)
    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.membershipPackage
        WHERE s.member.userId = :memberId
          AND s.status = 'ACTIVE'
          AND s.endDate > :now
        ORDER BY s.endDate ASC
        """)
    List<Subscription> findActiveNotEnded(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    boolean existsByMembershipPackage_PackageId(Long packageId);

    @Query("""
        SELECT COUNT(s) > 0 FROM Subscription s
        WHERE s.member.userId = :memberId
          AND s.membershipPackage.packageType = com.fptu.swp391.sportscentermanager.enums.PackageType.MEMBERSHIP
          AND (s.status = 'WAITING_ACTIVATION'
               OR (s.status = 'ACTIVE' AND s.endDate > :now))
        """)
    boolean existsUsableMembership(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    // Gói đã trả tiền mà chưa hết: chờ kích hoạt, hoặc ACTIVE còn hạn (cảnh báo mua trùng)
    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.membershipPackage
        WHERE s.member.userId = :memberId
          AND (s.status = 'WAITING_ACTIVATION'
               OR (s.status = 'ACTIVE' AND s.endDate > :now))
        """)
    List<Subscription> findOwnedNotEnded(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    // Membership đang dùng được tại thời điểm now
    @Query("""
        select count(s) > 0 from Subscription s
                where s.member.userId = :memberId
                        and s.status = 'ACTIVE'
                                and s.membershipPackage.packageType = com.fptu.swp391.sportscentermanager.enums.PackageType.MEMBERSHIP
                                        and s.startDate <= :now and s.endDate > :now
        """)
    boolean existsCurrentMembership(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    // Membership chờ kích hoạt của member, trả tiền trước đứng trước
    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.membershipPackage p
        WHERE s.member.userId = :memberId
          AND s.status = 'WAITING_ACTIVATION'
          AND p.packageType = com.fptu.swp391.sportscentermanager.enums.PackageType.MEMBERSHIP
        ORDER BY s.paidAt ASC
        """)
    List<Subscription> findWaitingMemberships(@Param("memberId") Long memberId);

    // Membership đã trả tiền trước mốc cutoff mà vẫn chưa kích hoạt (scheduler 14 ngày)
    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.member
        JOIN FETCH s.membershipPackage p
        WHERE s.status = 'WAITING_ACTIVATION'
          AND p.packageType = com.fptu.swp391.sportscentermanager.enums.PackageType.MEMBERSHIP
          AND s.paidAt <= :cutoff
        """)
    List<Subscription> findWaitingMembershipsPaidBefore(@Param("cutoff") LocalDateTime cutoff);

    // Gói lớp của các member đang học lớp này
    @Query("""
        SELECT r.subscription FROM ClassRegistration r
        WHERE r.sportClass.classId = :classId
          AND r.status = 'REGISTERED'
          AND r.subscription IS NOT NULL
        """)
    List<Subscription> findClassPackagesOfClass(@Param("classId") Long classId);

    @Query("""
            select r from ClassRegistration r
            join fetch r.sportClass
            where r.subscription.subscriptionId = :subscriptionId
            and r.status in ('REGISTERED', 'STOPPED')
        """)
    Optional<ClassRegistration> findLiveRegistration(@Param("subscriptionId") Long subscriptionId);
}
