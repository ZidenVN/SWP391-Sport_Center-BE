package com.fptu.swp391.sportscentermanager.config;

import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.repository.NotificationAccountRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Nhắc hội viên gia hạn khi gói tập còn <= REMIND_DAYS ngày.
// Chạy mỗi sáng 8h và một lần khi khởi động; mỗi hội viên nhận tối đa 1 lần nhắc / REPEAT_EVERY_DAYS ngày.
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SubscriptionExpiryReminder {
    static final String TITLE = "Gói tập sắp hết hạn";
    private static final int REMIND_DAYS = 7;
    private static final int REPEAT_EVERY_DAYS = 2;

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationAccountRepository notificationAccountRepository;
    private final NotificationService notificationService;

    // gọi nội bộ không đi qua proxy nên cần @Transactional ngay tại đây
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        remindExpiringSubscriptions();
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void remindExpiringSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusDays(REMIND_DAYS);
        int sent = 0;

        for (Member member : subscriptionRepository.findMembersWithSubscriptionEndingBetween(now, until)) {
            // Đã gia hạn nối tiếp thì gói xa nhất sẽ hết hạn sau "until" -> không cần nhắc
            Subscription latest = subscriptionRepository
                .findFirstByMember_UserIdAndStatusOrderByEndDateDesc(member.getUserId(), "ACTIVE")
                .orElse(null);
            if (latest == null || latest.getEndDate().isAfter(until)) continue;

            boolean remindedRecently = notificationAccountRepository
                .existsByUser_UserIdAndNotification_TitleAndNotification_SentDateAfter(member.getUserId(), TITLE, now.minusDays(REPEAT_EVERY_DAYS));
            if (remindedRecently) continue;

            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), latest.getEndDate().toLocalDate());
            String when = daysLeft <= 0 ? "hôm nay" : "sau " + daysLeft + " ngày";
            notificationService.sendSystemNotification(List.of(member), TITLE,
                "Gói " + latest.getMembershipPackage().getPackageName() + " của bạn sẽ hết hạn " + when
                    + " (" + latest.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "). Gia hạn ngay để không gián đoạn việc tập luyện.");
            sent++;
        }
        if (sent > 0) log.info("Đã gửi {} thông báo nhắc gia hạn gói tập", sent);
    }
}
