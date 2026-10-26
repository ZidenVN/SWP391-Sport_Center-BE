package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.ClassRegistrationRepository;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.SportClassRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
import com.fptu.swp391.sportscentermanager.service.ClassRegistrationService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.enums.PackageType;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
@Service
@RequiredArgsConstructor
public class ClassRegistrationServiceImpl implements ClassRegistrationService {

    private static final String REGISTERED = "REGISTERED";
    private static final String CANCELLED = "CANCELLED";

    private final ClassRegistrationRepository registrationRepository;
    private final SportClassRepository sportClassRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    @Value("${app.booking.cancel-cutoff-hours}")
    private long cancelCutoffHours;

    @Override
    @Transactional
    public ClassRegistrationResponseDTO register(Long memberId, Long classId) {
        // tại sao trong hàm register lại dùng findByIdForUpdate để xử lí trường hợp có nhiều request đến cùng lúc.
        /* study case: lớp A còn 1 slot cuối cùng (19/20slot)
        - An và Bình cùng nhấn đăng kí cùng một giây, 2 request chạy song song.
        - để giải quyết case này thì phải sử dụng kỹ thuật PESSIMICTIC LOCKING - nghĩa là khi đọc dòng dòng đó trong DB thì khoá luôn dòng đó lại, request khác muốn đọc thì phải đợi request trước xong.
        - Cách nó hoạt động: Khoá member + lớp (Pessimistic Locking) để các request đăng ký xếp hàng, tránh race condition.
         */
        Member member = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));

        // Khóa dòng của lớp để việc đếm chỗ + ghi đăng ký là một khối không bị chen ngang
        SportClass sportClass = sportClassRepository.findByIdForUpdate(classId)
            .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        // 1. Lớp phải còn mở và chưa diễn ra
        if (!"OPENING".equals(sportClass.getStatus())
            || sportClass.getStartTime() == null
            || !sportClass.getStartTime().isAfter(now)) {
            throw new AppException(ErrorCode.CLASS_NOT_OPEN);
        }

        // 2. Check Membership Status (include): phải có gói CLASS đúng môn, còn hạn, còn buổi (BR-REG-02)
        // Danh sách đã sắp gói hết hạn sớm nhất lên đầu (BR-REG-04)
        Long subjectId = sportClass.getSubject() != null ? sportClass.getSubject().getSubjectId() : null;
        List<Long> validSubscriptionIds = subscriptionRepository
            .findValidIdsForClass(memberId, subjectId, sportClass.getStartTime());
        if (validSubscriptionIds.isEmpty()) {
            throw new AppException(noValidSubscriptionReason(memberId, subjectId, sportClass.getStartTime(), now));
        }

        // 3. Không đăng ký trùng
        ClassRegistration existing = registrationRepository
            .findByMember_UserIdAndSportClass_ClassId(memberId, classId)
            .orElse(null);
        if (existing != null && REGISTERED.equals(existing.getStatus())) {
            throw new AppException(ErrorCode.ALREADY_REGISTERED);
        }

        if (registrationRepository.existsMemberScheduleConflict(memberId, classId, sportClass.getStartTime(), sportClass.getEndTime())) {
            throw new AppException(ErrorCode.MEMBER_SCHEDULE_CONFLICT);
        }

        // 4. Còn chỗ không
        long taken = registrationRepository.countBySportClass_ClassIdAndStatus(classId, REGISTERED);
        if (taken >= sportClass.getMaxCapacity()) {
            throw new AppException(ErrorCode.CLASS_FULL);
        }

        // 5. Trừ 1 buổi của gói đã chọn (BR-REG-05). Khoá dòng gói để không bị trừ/hoàn chen ngang (BR-REG-08)
        Subscription subscription = subscriptionRepository.findByIdForUpdate(validSubscriptionIds.get(0))
            .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
        if (!subscription.hasSessionLeft()) {
            throw new AppException(ErrorCode.NO_SESSIONS_LEFT);
        }
        subscription.setUsedSessions(subscription.getUsedSessions() + 1);
        subscriptionRepository.save(subscription);

        // 6. Ghi đăng ký (dùng lại dòng cũ nếu trước đó đã hủy)
        ClassRegistration registration = (existing != null) ? existing
            : ClassRegistration.builder().member(member).sportClass(sportClass).build();
        registration.setStatus(REGISTERED);
        registration.setRegisteredAt(now);
        registration.setCancelledAt(null);
        registration.setSubscription(subscription);

        return toResponse(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public ClassRegistrationResponseDTO cancel(Long memberId, Long registrationId) {
        ClassRegistration registration = registrationRepository
            .findByRegistrationIdAndMember_UserId(registrationId, memberId)
            .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));

        // huỷ lần 2: trả về trạng thái hiện tại, không báo lỗi idempotent
        if (CANCELLED.equals(registration.getStatus())) {
            return toResponse(registration);
        }

        LocalDateTime start = registration.getSportClass().getStartTime();
        if (start == null || LocalDateTime.now().isAfter(start.minusHours(cancelCutoffHours))) {
            throw new AppException(ErrorCode.CANCEL_NOT_ALLOWED);
        }

        registration.setStatus(CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        // BR-CAN-01: huỷ trước cutoff thì hoàn 1 buổi vào đúng gói đã trừ
        subscriptionService.refundSession(registration);
        return toResponse(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public List<ClassRegistrationResponseDTO> getHistory(Long memberId) {
        requireMember(memberId);
        return registrationRepository.findHistory(memberId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public List<ClassRegistrationResponseDTO> getUpcomingSchedule(Long memberId) {
        requireMember(memberId);
        return registrationRepository.findUpcoming(memberId, LocalDateTime.now())
            .stream().map(this::toResponse).toList();
    }
    // Thứ tự báo lỗi khi không có gói hợp lệ (bảng mục 3.3)
    private ErrorCode noValidSubscriptionReason(Long memberId, Long subjectId, LocalDateTime classTime, LocalDateTime now) {
        List<Subscription> covering = subscriptionRepository.findCovering(memberId, classTime);
        if (covering.isEmpty()) {
            boolean hasCurrent = !subscriptionRepository.findCurrent(memberId, now).isEmpty();
            return hasCurrent ? ErrorCode.SUBSCRIPTION_EXPIRED : ErrorCode.NO_ACTIVE_SUBSCRIPTION;
        }
        List<Subscription> classPackages = covering.stream()
            .filter(s -> s.getMembershipPackage().getPackageType() == PackageType.CLASS)
            .toList();
        if (classPackages.isEmpty()) {
            return ErrorCode.PACKAGE_NOT_ALLOW_CLASS;
        }
        boolean subjectCovered = classPackages.stream()
            .anyMatch(s -> s.getMembershipPackage().getSubjects().stream()
                .anyMatch(sub -> sub.getSubjectId().equals(subjectId)));
        return subjectCovered ? ErrorCode.NO_SESSIONS_LEFT : ErrorCode.SUBJECT_NOT_COVERED;
    }
    private void requireMember(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private ClassRegistrationResponseDTO toResponse(ClassRegistration r) {
        SportClass c = r.getSportClass();
        return ClassRegistrationResponseDTO.builder()
            .registrationId(r.getRegistrationId())
            .memberId(r.getMember().getUserId())
            .classId(c.getClassId())
            .imageUrl(c.getImageUrl())
            .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
            .roomName(c.getRoom() != null ? c.getRoom().getRoomName() : null)
            .coachName(c.getCoach() != null
                ? c.getCoach().getFirstName() + " " + c.getCoach().getLastName() : "Chưa có HLV")
            .startTime(c.getStartTime())
            .endTime(c.getEndTime())
            .status(r.getStatus())
            .registeredAt(r.getRegisteredAt())
            .cancelledAt(r.getCancelledAt())
            .subscriptionId(r.getSubscription() != null ? r.getSubscription().getSubscriptionId() : null)
            .usedSessions(r.getSubscription() != null ? r.getSubscription().getUsedSessions() : null)
            .remainingSessions(r.getSubscription() != null ? r.getSubscription().remainingSessions() : null)
            .build();
    }


}
