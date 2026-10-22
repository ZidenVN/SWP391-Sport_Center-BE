package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.NotificationAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationAccountRepository  extends JpaRepository<NotificationAccount, Long> {
    List<NotificationAccount> findByUser_UserIdOrderByNotification_SentDateDesc(Long userId);

    // Đã gửi thông báo có tiêu đề này cho user kể từ thời điểm "since" chưa (chống gửi trùng)
    boolean existsByUser_UserIdAndNotification_TitleAndNotification_SentDateAfter(Long userId, String title, LocalDateTime since);
}
