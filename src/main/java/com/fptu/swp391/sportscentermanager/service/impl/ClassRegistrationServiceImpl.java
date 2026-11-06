package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.EligibleClassResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MemberSessionResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import com.fptu.swp391.sportscentermanager.entity.ClassSession;
import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.PackageType;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.ClassRegistrationRepository;
import org.springframework.transaction.annotation.Isolation;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.ScheduleQueryRepository;
import com.fptu.swp391.sportscentermanager.repository.SportClassRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.service.ClassRegistrationService;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClassRegistrationServiceImpl implements ClassRegistrationService {

    private static final String REGISTERED = "REGISTERED";
    private static final String SWITCHED = "SWITCHED";
    private static final String CANCELLED = "CANCELLED";
    private static final String STOPPED = "STOPPED";
    // Hội viên tự đổi lớp khi buổi đầu còn hơn 12 tiếng
    private static final long SELF_SWITCH_HOURS = 12;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClassRegistrationRepository registrationRepository;
    private final SportClassRepository sportClassRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ScheduleQueryRepository scheduleQueryRepository;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;

    // ---------- Trạng thái gói (Subscription của A; nếu A đổi status sang enum thì chỉ sửa 2 hàm này) ----------
    private boolean isWaiting(Subscription sub) {
        return "WAITING_ACTIVATION".equals(String.valueOf(sub.getStatus()));
    }

    private boolean isActive(Subscription sub) {
        return "ACTIVE".equals(String.valueOf(sub.getStatus()));
    }

    // ---------- Lớp đăng ký được ----------
    @Override
    @Transactional(readOnly = true)
    public List<EligibleClassResponseDTO> getEligibleClasses(Long memberId, Long subscriptionId) {
        Subscription sub = requireOwnedClassPackage(memberId, subscriptionId);
        if (!isWaiting(sub) && !isActive(sub)) {
            throw new AppException(ErrorCode.REGISTRATION_PACKAGE_NOT_WAITING);
        }
        MembershipPackage pkg = sub.getMembershipPackage();

        // Gói đang có lớp (ACTIVE) thì đây là danh sách để đổi lớp: bỏ lớp đang học, bỏ qua trùng lịch với nó
        Long currentClassId = scheduleQueryRepository.findRegisteredClassIdsBySubscription(subscriptionId)
            .stream().findFirst().orElse(-1L);

        List<SportClass> classes = scheduleQueryRepository.findJoinableClasses(
            pkg.getSubject().getSubjectId(), pkg.getDurationDays(), LocalDateTime.now());
        Map<Long, Long> taken = countTaken(classes);

        List<EligibleClassResponseDTO> result = new ArrayList<>();
        for (SportClass c : classes) {
            long registered = taken.getOrDefault(c.getClassId(), 0L);
            if (c.getClassId().equals(currentClassId)) continue;
            if (registered >= c.getMaxCapacity()) continue;
            if (scheduleQueryRepository.existsMemberSessionConflict(memberId, c.getClassId(), currentClassId)) continue;
            result.add(toEligible(c, registered));
        }
        return result;
    }

    // ---------- Đăng ký ----------
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ClassRegistrationResponseDTO registerSelf(Long memberId, Long classId, Long subscriptionId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getQuizCompletedAt() == null) {
            throw new AppException(ErrorCode.QUIZ_NOT_COMPLETED);
        }
        // Gọi trong cùng transaction; register vẫn khoá member + lớp + gói bằng SELECT ... FOR UPDATE
        return register(memberId, classId, subscriptionId);
    }

    @Override
    @Transactional
    public ClassRegistrationResponseDTO register(Long memberId, Long classId, Long subscriptionId) {
        // Khoá member + lớp + gói (pessimistic lock) để các request đăng ký xếp hàng, tránh tranh chỗ cuối
        Member member = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        SportClass sportClass = sportClassRepository.findByIdForUpdate(classId)
            .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        Subscription sub = lockOwnedClassPackage(memberId, subscriptionId);
        LocalDateTime now = LocalDateTime.now();

        // Gói lớp đang chờ mới đăng ký được; gói đã có lớp thì phải đổi lớp
        if (!isWaiting(sub)) {
            throw new AppException(ErrorCode.REGISTRATION_PACKAGE_NOT_WAITING);
        }
        requireJoinable(sportClass, sub.getMembershipPackage(), now);

        ClassRegistration existing = registrationRepository
            .findByMember_UserIdAndSportClass_ClassId(memberId, classId).orElse(null);
        if (existing != null && REGISTERED.equals(existing.getStatus())) {
            throw new AppException(ErrorCode.ALREADY_REGISTERED);
        }
        requireNoConflictAndSeat(memberId, sportClass, -1L);

        ClassRegistration registration = saveRegistered(existing, member, sportClass, sub, now);

        // Gói lớp ACTIVE tới ngày lớp kết thúc; membership đang chờ thì kích hoạt luôn
        subscriptionService.activateClassPackage(sub, sportClass.getEndDate());
        subscriptionService.activateMembershipIfWaiting(memberId, now);
        return toResponse(registration);
    }

    // ---------- Huỷ đăng ký ----------
    @Override
    @Transactional
    public ClassRegistrationResponseDTO cancel(Long memberId, Long registrationId) {
        ClassRegistration registration = registrationRepository
            .findByRegistrationIdAndMember_UserId(registrationId, memberId)
            .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));

        // huỷ lần 2: trả về trạng thái hiện tại, không báo lỗi (idempotent)
        if (CANCELLED.equals(registration.getStatus())) {
            return toResponse(registration);
        }
        if (!REGISTERED.equals(registration.getStatus())) {
            throw new AppException(ErrorCode.REGISTRATION_NOT_ACTIVE);
        }
        LocalDateTime now = LocalDateTime.now();
        if (hasStarted(registration.getSportClass(), now)) {
            throw new AppException(ErrorCode.REGISTRATION_CLASS_STARTED);
        }

        registration.setStatus(CANCELLED);
        registration.setCancelledAt(now);
        // Gói lớp quay về chờ, membership đã kích hoạt thì giữ nguyên
        if (registration.getSubscription() != null) {
            subscriptionService.releaseClassPackage(registration.getSubscription());
        }
        return toResponse(registrationRepository.save(registration));
    }

    // ---------- Đổi lớp ----------
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ClassRegistrationResponseDTO switchSelf(Long memberId, Long registrationId, Long newClassId) {
        ClassRegistration current = registrationRepository
            .findByRegistrationIdAndMember_UserId(registrationId, memberId)
            .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));
        if (!REGISTERED.equals(current.getStatus())) {
            throw new AppException(ErrorCode.REGISTRATION_NOT_ACTIVE);
        }
        LocalDateTime now = LocalDateTime.now();
        if (!canSelfSwitch(current.getSportClass(), now)) {
            throw new AppException(ErrorCode.REGISTRATION_SELF_SWITCH_CLOSED);
        }
        User actor = userRepository.findById(memberId)
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        return doSwitch(current, newClassId, actor, false, now);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ClassRegistrationResponseDTO switchByReceptionist(Long receptionistId, Long registrationId, Long newClassId) {
        ClassRegistration current = registrationRepository.findById(registrationId)
            .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();

        boolean countTurn;
        boolean classStopped = STOPPED.equals(current.getSportClass().getStatus());
        if (STOPPED.equals(current.getStatus()) || (REGISTERED.equals(current.getStatus()) && classStopped)) {
            // Lớp bị dừng: chuyển lớp không tính lượt
            countTurn = false;
        } else if (REGISTERED.equals(current.getStatus())) {
            // Chỉ tính lượt khi lớp đã bắt đầu; trước đó lễ tân đổi giúp không tính lượt
            countTurn = hasStarted(current.getSportClass(), now);
        } else {
            throw new AppException(ErrorCode.REGISTRATION_NOT_ACTIVE);
        }
        User actor = userRepository.findById(receptionistId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        ClassRegistrationResponseDTO result = doSwitch(current, newClassId, actor, countTurn, now);

        notificationService.notifyUsers(List.of(current.getMember()), "Đổi lớp thành công",
            "Lễ tân đã chuyển bạn sang lớp " + result.getSubjectName()
                + " bắt đầu " + result.getStartDate().format(DATE) + ".", "CLASS_SWITCHED");
        return result;
    }

    private ClassRegistrationResponseDTO doSwitch(ClassRegistration current, Long newClassId, User actor,
                                                  boolean countTurn, LocalDateTime now) {
        Long oldClassId = current.getSportClass().getClassId();
        if (oldClassId.equals(newClassId)) {
            throw new AppException(ErrorCode.REGISTRATION_SAME_CLASS);
        }
        Long memberId = current.getMember().getUserId();
        Member member = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        SportClass newClass = sportClassRepository.findByIdForUpdate(newClassId)
            .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        if (current.getSubscription() == null) {
            throw new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
        Subscription sub = lockOwnedClassPackage(memberId, current.getSubscription().getSubscriptionId());

        if (countTurn) {
            int used = sub.getReceptionistSwitchCount() == null ? 0 : sub.getReceptionistSwitchCount();
            if (used >= 1) {
                throw new AppException(ErrorCode.REGISTRATION_SWITCH_LIMIT);
            }
            sub.setReceptionistSwitchCount(used + 1);
        }

        // Lớp mới: cùng môn, cùng thời hạn, đang mở, chưa bắt đầu, còn chỗ, không trùng lịch (bỏ qua lớp cũ)
        requireJoinable(newClass, sub.getMembershipPackage(), now);
        ClassRegistration existing = registrationRepository
            .findByMember_UserIdAndSportClass_ClassId(memberId, newClassId).orElse(null);
        if (existing != null && REGISTERED.equals(existing.getStatus())) {
            throw new AppException(ErrorCode.ALREADY_REGISTERED);
        }
        requireNoConflictAndSeat(memberId, newClass, oldClassId);

        // Đăng ký cũ giữ lại làm lịch sử
        current.setStatus(SWITCHED);
        current.setSwitchedAt(now);
        current.setSwitchedBy(actor);
        registrationRepository.save(current);

        ClassRegistration registration = saveRegistered(existing, member, newClass, sub, now);
        subscriptionRepository.save(sub);
        // Hạn gói lớp chuyển theo lớp mới
        subscriptionService.activateClassPackage(sub, newClass.getEndDate());
        return toResponse(registration);
    }

    // ---------- Lịch ----------
    @Override
    @Transactional(readOnly = true)
    public List<ClassRegistrationResponseDTO> getHistory(Long memberId) {
        requireMember(memberId);
        return registrationRepository.findHistory(memberId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberSessionResponseDTO> getUpcomingSchedule(Long memberId) {
        requireMember(memberId);
        return scheduleQueryRepository.findMemberUpcomingSessions(memberId, LocalDateTime.now())
            .stream().map(this::toSessionResponse).toList();
    }

    // ---------- Hàm cho B gọi (A hoàn tiền tự xử lý đăng ký trong PaymentServiceImpl) ----------
    @Override
    @Transactional
    public void releaseForCancelledClass(Long classId) {
        LocalDateTime now = LocalDateTime.now();
        for (ClassRegistration r : registrationRepository.findByClassAndStatus(classId, REGISTERED)) {
            r.setStatus(CANCELLED);
            r.setCancelledAt(now);
            if (r.getSubscription() != null) {
                subscriptionService.releaseClassPackage(r.getSubscription());
            }
        }
    }

    @Override
    @Transactional
    public void stopRegistrations(Long classId) {
        // Hội viên chọn sau: lễ tân chuyển lớp (không tính lượt) hoặc Manager hoàn tiền theo số buổi chưa học
        for (ClassRegistration r : registrationRepository.findByClassAndStatus(classId, REGISTERED)) {
            r.setStatus(STOPPED);
        }
    }

    // ---------- Hàm phụ ----------

    // Gói phải thuộc hội viên và là gói lớp có đủ môn + thời hạn
    private Subscription requireOwnedClassPackage(Long memberId, Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
            .filter(s -> s.getMember().getUserId().equals(memberId))
            .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
        checkClassPackage(sub);
        return sub;
    }

    private Subscription lockOwnedClassPackage(Long memberId, Long subscriptionId) {
        Subscription sub = subscriptionRepository.findByIdForUpdate(subscriptionId)
            .filter(s -> s.getMember().getUserId().equals(memberId))
            .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
        checkClassPackage(sub);
        return sub;
    }

    private void checkClassPackage(Subscription sub) {
        MembershipPackage pkg = sub.getMembershipPackage();
        if (pkg == null || pkg.getPackageType() != PackageType.CLASS
            || pkg.getSubject() == null || pkg.getDurationDays() == null) {
            throw new AppException(ErrorCode.REGISTRATION_PACKAGE_NOT_CLASS);
        }
    }

    // Lớp chưa có buổi đầu (dữ liệu cũ) coi như đã bắt đầu
    private boolean hasStarted(SportClass c, LocalDateTime now) {
        return c.getFirstSessionAt() == null || !c.getFirstSessionAt().isAfter(now);
    }

    private boolean canSelfSwitch(SportClass c, LocalDateTime now) {
        return c.getFirstSessionAt() != null && c.getFirstSessionAt().minusHours(SELF_SWITCH_HOURS).isAfter(now);
    }

    // Lớp đăng ký được với gói này: đang mở, chưa bắt đầu, cùng môn, cùng thời hạn
    private void requireJoinable(SportClass c, MembershipPackage pkg, LocalDateTime now) {
        if (!"OPENING".equals(c.getStatus())) {
            throw new AppException(ErrorCode.CLASS_NOT_OPEN);
        }
        if (hasStarted(c, now)) {
            throw new AppException(ErrorCode.REGISTRATION_CLASS_STARTED);
        }
        if (c.getSubject() == null || !c.getSubject().getSubjectId().equals(pkg.getSubject().getSubjectId())) {
            throw new AppException(ErrorCode.REGISTRATION_SUBJECT_MISMATCH);
        }
        if (!Objects.equals(c.getDurationDays(), pkg.getDurationDays())) {
            throw new AppException(ErrorCode.REGISTRATION_DURATION_MISMATCH);
        }
    }

    // Không trùng lịch với lớp khác đang học (bỏ qua ignoreClassId), lớp còn chỗ. Gọi sau khi đã khoá dòng lớp
    private void requireNoConflictAndSeat(Long memberId, SportClass c, Long ignoreClassId) {
        if (scheduleQueryRepository.existsMemberSessionConflict(memberId, c.getClassId(), ignoreClassId)) {
            throw new AppException(ErrorCode.MEMBER_SCHEDULE_CONFLICT);
        }
        long taken = registrationRepository.countBySportClass_ClassIdAndStatus(c.getClassId(), REGISTERED);
        if (taken >= c.getMaxCapacity()) {
            throw new AppException(ErrorCode.CLASS_FULL);
        }
    }

    // Ghi đăng ký REGISTERED, dùng lại dòng cũ của cặp (member, lớp) nếu có
    private ClassRegistration saveRegistered(ClassRegistration existing, Member member, SportClass c,
                                             Subscription sub, LocalDateTime now) {
        ClassRegistration registration = (existing != null) ? existing
            : ClassRegistration.builder().member(member).sportClass(c).build();
        registration.setStatus(REGISTERED);
        registration.setRegisteredAt(now);
        registration.setCancelledAt(null);
        registration.setSwitchedAt(null);
        registration.setSwitchedBy(null);
        registration.setReminderSentAt(null);
        registration.setSubscription(sub);
        return registrationRepository.save(registration);
    }

    // Đếm chỗ đã giữ của nhiều lớp bằng 1 câu SQL
    private Map<Long, Long> countTaken(List<SportClass> classes) {
        Map<Long, Long> taken = new HashMap<>();
        if (classes.isEmpty()) return taken;
        List<Long> ids = classes.stream().map(SportClass::getClassId).toList();
        for (Object[] row : registrationRepository.countActiveByClassIds(ids)) {
            taken.put((Long) row[0], (Long) row[1]);
        }
        return taken;
    }

    private void requireMember(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private String fullName(User u) {
        return u == null ? null : u.getFirstName() + " " + u.getLastName();
    }

    private EligibleClassResponseDTO toEligible(SportClass c, long registered) {
        return EligibleClassResponseDTO.builder()
            .classId(c.getClassId())
            .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
            .coachName(c.getCoach() != null ? fullName(c.getCoach()) : "Chưa có HLV")
            .roomName(c.getRoom() != null ? c.getRoom().getRoomName() : null)
            .imageUrl(c.getImageUrl())
            .startDate(c.getStartDate())
            .endDate(c.getEndDate())
            .durationDays(c.getDurationDays())
            .daysOfWeek(c.getDaysOfWeek())
            .startClock(c.getStartClock())
            .endClock(c.getEndClock())
            .firstSessionAt(c.getFirstSessionAt())
            .maxCapacity(c.getMaxCapacity())
            .registeredCount(registered)
            .build();
    }

    private MemberSessionResponseDTO toSessionResponse(ClassSession s) {
        SportClass c = s.getSportClass();
        Coach main = c.getCoach();
        Coach teacher = s.getCoach();
        return MemberSessionResponseDTO.builder()
            .sessionId(s.getSessionId())
            .classId(c.getClassId())
            .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
            .roomName(s.getRoom() != null ? s.getRoom().getRoomName() : null)
            .coachName(teacher != null ? fullName(teacher) : "Chưa có HLV")
            .substitute(teacher != null && main != null && !teacher.getUserId().equals(main.getUserId()))
            .mainCoachName(fullName(main))
            .type(s.getType().name())
            .status(s.getStatus().name())
            .startTime(s.getStartTime())
            .endTime(s.getEndTime())
            .note(s.getNote())
            .build();
    }

    private ClassRegistrationResponseDTO toResponse(ClassRegistration r) {
        SportClass c = r.getSportClass();
        Subscription sub = r.getSubscription();
        return ClassRegistrationResponseDTO.builder()
            .registrationId(r.getRegistrationId())
            .memberId(r.getMember().getUserId())
            .classId(c.getClassId())
            .imageUrl(c.getImageUrl())
            .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
            .roomName(c.getRoom() != null ? c.getRoom().getRoomName() : null)
            .coachName(c.getCoach() != null ? fullName(c.getCoach()) : "Chưa có HLV")
            .startTime(c.getStartTime())
            .endTime(c.getEndTime())
            .startDate(c.getStartDate())
            .endDate(c.getEndDate())
            .daysOfWeek(c.getDaysOfWeek())
            .startClock(c.getStartClock())
            .endClock(c.getEndClock())
            .firstSessionAt(c.getFirstSessionAt())
            .classStatus(c.getStatus())
            .status(r.getStatus())
            .registeredAt(r.getRegisteredAt())
            .cancelledAt(r.getCancelledAt())
            .switchedAt(r.getSwitchedAt())
            .subscriptionId(sub != null ? sub.getSubscriptionId() : null)
            .packageName(sub != null && sub.getMembershipPackage() != null ? sub.getMembershipPackage().getPackageName() : null)
            .receptionistSwitchCount(sub != null ? sub.getReceptionistSwitchCount() : null)
            .build();
    }
}
