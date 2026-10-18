package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.NotificationAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationAccountRepository  extends JpaRepository<NotificationAccount, Long> {
    List<NotificationAccount> findByUser_UserIdOrderByNotification_SentDateDesc(Long userId);
}
