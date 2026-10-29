package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.NotificationAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationAccountRepository  extends JpaRepository<NotificationAccount, Long> {
    @EntityGraph(attributePaths = {"notification", "notification.sender"})
    List<NotificationAccount> findByUser_UserIdOrderByNotification_SentDateDesc(Long userId);

    long countByUser_UserIdAndReadAtIsNull(Long userId);

    Optional<NotificationAccount> findByNotification_NotificationIdAndUser_UserId(Long notificationId, Long userId);

    @Modifying
    @Query("UPDATE NotificationAccount na SET na.readAt = :now WHERE na.user.userId = :userId AND na.readAt IS NULL")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

}
