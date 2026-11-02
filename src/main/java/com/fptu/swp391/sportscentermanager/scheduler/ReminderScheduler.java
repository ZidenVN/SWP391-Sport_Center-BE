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
import com.fptu.swp391.sportscentermanager.entity.ClassSession;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.enums.PackageType;
import com.fptu.swp391.sportscentermanager.enums.SessionType;
import com.fptu.swp391.sportscentermanager.repository.ScheduleQueryRepository;
import java.time.temporal.ChronoUnit;
@Slf4j
@Component
@RequiredArgsConstructor

public class ReminderScheduler {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private final SubscriptionRepository subscriptionRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final ScheduleQueryRepository scheduleQueryRepository;
    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;

    @Value("${app.reminder.subscription-days-before:3}")
    private int subscriptionDaysBefore;

    @Value("${app.reminder.class-hours-before:24}")
    private int classHoursBefore;

    // Phải bằng chu kỳ của app.reminder.class-cron (mặc định 15 phút)
    @Value("${app.reminder.class-interval-minutes:15}")
    private int classIntervalMinutes;
    @Scheduled(cron = "${app.reminder.subscription-cron:0 0 8 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void remindExpiringSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> subs = subscriptionRepository.findExpiringWithoutReminder(now, now.plusDays(subscriptionDaysBefore));

        int sent = 0;
        for (Subscription s : subs) {
            // BR-SUB-07: đã gia hạn gói cùng loại + cùng môn thì không nhắc (không đánh dấu, lỡ gói gia hạn bị hoàn tiền vẫn nhắc được)
            // Chỉ nhắc membership; gói lớp hết hạn theo lớp nên không nhắc gia hạn
            if (s.getMembershipPackage().getPackageType() != PackageType.MEMBERSHIP) continue;
            if (subscriptionService.hasRenewal(s)) continue;
            String content = s.getMembershipPackage().getPackageName() + " của bạn sẽ hết hạn vào " + s.getEndDate().format(DATE) + ". Hãy gia hạn để không bị gián đoạn lịch tập.";
            notificationService.notifyUsers(List.of(s.getMember()), "Gói tập sắp hết hạn", content, "SUBSCRIPTION_EXPIRING");
            s.setExpiryReminderSentAt(now);
            sent++;
        }
        log.info("Subscription reminders sent: {}", sent);
    }

    // Nhắc theo buổi: mỗi lần chạy lấy các buổi bắt đầu trong đúng 1 khung = chu kỳ cron,
    // cách bây giờ classHoursBefore giờ. Mỗi buổi rơi vào đúng 1 khung nên không cần cột đánh dấu đã nhắc
    @Scheduled(cron = "${app.reminder.class-cron:0 */15 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void remindUpcomingClasses() {
        LocalDateTime to = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).plusHours(classHoursBefore);
        LocalDateTime from = to.minusMinutes(classIntervalMinutes);
        int sent = 0;
        for (ClassSession s : scheduleQueryRepository.findSessionsStartingBetween(from, to)) {
            SportClass c = s.getSportClass();
            List<Member> members = classRegistrationRepository.findActiveByClassId(c.getClassId()).stream()
                .map(ClassRegistration::getMember).toList();
            if (members.isEmpty()) continue;
            String subject = c.getSubject() != null ? c.getSubject().getSubjectName() : "Lớp học";
            String room = s.getRoom() != null ? s.getRoom().getRoomName() : "Chưa xếp phòng";
            String content = (s.getType() == SessionType.MAKEUP ? "Buổi bù lớp " : "Lớp ") + subject
                + " bắt đầu lúc " + s.getStartTime().format(DATE_TIME) + " tại " + room
                + (s.getCoach() != null ? ", HLV " + s.getCoach().getFirstName() + " " + s.getCoach().getLastName() : "") + ".";
            notificationService.notifyUsers(members, "Nhắc lịch học", content, "CLASS_REMINDER");
            sent += members.size();
        }
        log.info("Class reminders sent: {}", sent);
    }
}
