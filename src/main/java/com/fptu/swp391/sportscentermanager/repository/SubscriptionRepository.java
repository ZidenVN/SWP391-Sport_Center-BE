package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // Lịch sử đăng ký, mới nhất trước
    List<Subscription> findByMember_UserIdOrderByEndDateDesc(Long memberId);


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

    // Giành quyền gửi nhắc hết hạn (cùng cách với ClassSessionRepository.claimReminder)
    @Modifying
    @Query("update Subscription s set s.expiryReminderSentAt = :now where s.subscriptionId = :id and s.expiryReminderSentAt is null")
    int claimExpiryReminder(@Param("id") Long id, @Param("now") LocalDateTime now);

    // ===== Gói tập theo buổi =====

    // Khoá dòng gói (pessimistic lock) khi đăng ký / đổi lớp, tránh 2 request cùng sửa
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.subscriptionId = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);

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

    // Đăng ký đang dùng của nhiều gói lớp cùng lúc (1 query cho cả danh sách, tránh N+1)
    @Query("""
        SELECT r FROM ClassRegistration r
        JOIN FETCH r.sportClass c
        JOIN FETCH c.subject
        WHERE r.subscription.subscriptionId IN :ids
          AND r.status IN ('REGISTERED', 'STOPPED')
          AND c.status IN ('OPENING', 'STOPPED')
        """)
    List<ClassRegistration> findLiveRegistrationsOf(@Param("ids") Collection<Long> ids);
}
