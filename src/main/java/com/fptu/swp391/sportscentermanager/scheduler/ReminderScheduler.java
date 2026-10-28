package com.fptu.swp391.sportscentermanager.scheduler;

import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.repository.ClassRegistrationRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
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

public class ReminderScheduler {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private final SubscriptionRepository subscriptionRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;

    @Value("${app.reminder.subscription-days-before:3}")
    private int subscriptionDaysBefore;

    @Value("${app.reminder.class-hours-before:24}")
    private int classHoursBefore;

    @Scheduled(cron = "${app.reminder.subscription-cron:0 0 8 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void remindExpiringSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> subs = subscriptionRepository.findExpiringWithoutReminder(now, now.plusDays(subscriptionDaysBefore));

        int sent = 0;
        for (Subscription s : subs) {
            // BR-SUB-07: đã gia hạn gói cùng loại + cùng môn thì không nhắc (không đánh dấu, lỡ gói gia hạn bị hoàn tiền vẫn nhắc được)
            if (subscriptionService.hasRenewal(s)) continue;
            String content = s.getMembershipPackage().getPackageName() + " của bạn sẽ hết hạn vào " + s.getEndDate().format(DATE) + ". Hãy gia hạn để không bị gián đoạn lịch tập.";
            notificationService.notifyUsers(List.of(s.getMember()), "Gói tập sắp hết hạn", content, "SUBSCRIPTION_EXPIRING");
            s.setExpiryReminderSentAt(now);
            sent++;
        }
        log.info("Subscription reminders sent: {}", sent);
    }

    @Scheduled(cron = "${app.reminder.class-cron:0 */15 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void remindUpcomingClasses() {
        LocalDateTime now = LocalDateTime.now();
        List<ClassRegistration> regs = classRegistrationRepository.findUpcomingWithoutReminder(now, now.plusHours(classHoursBefore));
        for (ClassRegistration r : regs) {
            SportClass c = r.getSportClass();
            String subject = c.getSubject() != null ? c.getSubject().getSubjectName() : "Lớp học";
            String room = c.getRoom() != null ? c.getRoom().getRoomName() : "Chưa xếp phòng";
            String content = "Lớp " + subject + " bắt đầu lúc " + c.getStartTime().format(DATE_TIME) + " tại " + room + ".";
            notificationService.notifyUsers(List.of(r.getMember()), "Nhắc lịch học", content, "CLASS_REMINDER");
            r.setReminderSentAt(now);
        }
        log.info("Class reminders sent: {}", regs.size());
    }
}
