package com.fptu.swp391.sportscentermanager.scheduler;

import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.repository.ClassRegistrationRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
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

    @Value("${app.reminder.subscription-days-before:3}")
    private int subscriptionDaysBefore;

    @Value("${app.reminder.class-hours-before:24}")
    private int classHoursBefore;

    @Scheduled(cron = "${app.reminder.subscription-cron:0 0 8 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void remindExpiringSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> subs = subscriptionRepository.findExpiringWithoutReminder(now, now.plusDays(subscriptionDaysBefore));

        for (Subscription s : subs) {
            String content = s.getMembershipPackage().getPackageName() + " của bạn sẽ hết hạn vào " + s.getEndDate().format(DATE) + ". Hãy gia hạn để không bị gián đoạn lịch tập.";
            notificationService.notifyUsers(List.of(s.getMember()), "Gói tập sắp hết hạn", content, "SUBSCRIPTION_EXPIRING");
            s.setExpiryReminderSentAt(now);
        }
        log.info("Subscription reminders sent: {}", subs.size());
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
