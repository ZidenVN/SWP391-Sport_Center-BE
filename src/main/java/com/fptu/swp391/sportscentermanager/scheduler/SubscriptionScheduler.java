package com.fptu.swp391.sportscentermanager.scheduler;

import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.membership.auto-activate-days:14}")
    private int autoActivateDays;

    @Scheduled(cron = "${app.membership.auto-activate-cron:0 5 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void autoActivateMemberships() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> activated = subscriptionService.autoActivateWaitingMemberships(now, autoActivateDays);
        for (Subscription s : activated) {
            String content = s.getMembershipPackage().getPackageName() + " đã tự kích hoạt sau "
                + autoActivateDays + " ngày kể từ khi thanh toán, hạn sử dụng đến " + s.getEndDate().format(DATE) + ".";
            notificationService.notifyUsers(List.of(s.getMember()), "Gói membership đã được kích hoạt", content, "SUBSCRIPTION_ACTIVATED");
        }
        log.info("Auto-activated memberships: {}", activated.size());
    }
}
