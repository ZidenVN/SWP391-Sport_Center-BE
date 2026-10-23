package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.ClassAttendance;
import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.ClassAttendanceRepository;
import com.fptu.swp391.sportscentermanager.repository.ClassRegistrationRepository;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.SportClassRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
import com.fptu.swp391.sportscentermanager.service.ClassRegistrationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassRegistrationServiceImpl implements ClassRegistrationService {

    private static final String REGISTERED = "REGISTERED";
    private static final String CANCELLED = "CANCELLED";

    private final ClassRegistrationRepository registrationRepository;
    private final SportClassRepository sportClassRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ClassAttendanceRepository attendanceRepository;

    @Override
    @Transactional
    public ClassRegistrationResponseDTO register(Long memberId, Long classId) {
        Member member = memberRepository.findById(memberId)
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

        // 2. Check Membership Status (include): phải có gói còn hiệu lực
        Subscription current = subscriptionRepository.findCurrent(memberId, now)
            .stream().findFirst()
            .orElseThrow(() -> new AppException(ErrorCode.NO_ACTIVE_SUBSCRIPTION));
        // Ngày học phải nằm trong 1 gói đang hiệu lực: gói hiện tại hoặc gói gia hạn nối tiếp phía sau
        LocalDateTime classStart = sportClass.getStartTime();
        boolean covered = !classStart.isAfter(current.getEndDate())
            || subscriptionRepository.findByMember_UserIdAndStatus(memberId, "ACTIVE").stream()
                .anyMatch(s -> !classStart.isBefore(s.getStartDate()) && !classStart.isAfter(s.getEndDate()));
        if (!covered) {
            throw new AppException(ErrorCode.SUBSCRIPTION_NOT_COVER_CLASS);
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

        // 5. Ghi đăng ký (dùng lại dòng cũ nếu trước đó đã hủy)
        ClassRegistration registration = (existing != null) ? existing
            : ClassRegistration.builder().member(member).sportClass(sportClass).build();
        registration.setStatus(REGISTERED);
        registration.setRegisteredAt(now);
        registration.setCancelledAt(null);

        return toResponse(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public ClassRegistrationResponseDTO cancel(Long memberId, Long registrationId) {
        ClassRegistration registration = registrationRepository
            .findByRegistrationIdAndMember_UserId(registrationId, memberId)
            .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));

        if (CANCELLED.equals(registration.getStatus())) {
            throw new AppException(ErrorCode.REGISTRATION_ALREADY_CANCELLED);
        }
        // Lớp đã bắt đầu thì không hủy được nữa
        LocalDateTime start = registration.getSportClass().getStartTime();
        if (start == null || !start.isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.CANCEL_NOT_ALLOWED);
        }

        registration.setStatus(CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        return toResponse(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public List<ClassRegistrationResponseDTO> getHistory(Long memberId) {
        requireMember(memberId);
        Map<Long, ClassAttendance> attendance = attendanceByClass(memberId);
        return registrationRepository.findHistory(memberId).stream().map(r -> toResponse(r, attendance)).toList();
    }

    @Override
    @Transactional
    public List<ClassRegistrationResponseDTO> getUpcomingSchedule(Long memberId) {
        requireMember(memberId);
        Map<Long, ClassAttendance> attendance = attendanceByClass(memberId);
        return registrationRepository.findUpcoming(memberId, LocalDateTime.now())
            .stream().map(r -> toResponse(r, attendance)).toList();
    }

    private void requireMember(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    // classId -> điểm danh của hội viên, lấy 1 lần cho cả danh sách
    private Map<Long, ClassAttendance> attendanceByClass(Long memberId) {
        return attendanceRepository.findByMember_UserId(memberId).stream()
            .collect(Collectors.toMap(a -> a.getSportClass().getClassId(), Function.identity(), (a, b) -> b));
    }

    private ClassRegistrationResponseDTO toResponse(ClassRegistration r) {
        return toResponse(r, Map.of());
    }

    private ClassRegistrationResponseDTO toResponse(ClassRegistration r, Map<Long, ClassAttendance> attendance) {
        SportClass c = r.getSportClass();
        ClassAttendance a = attendance.get(c.getClassId());
        return ClassRegistrationResponseDTO.builder()
            .registrationId(r.getRegistrationId())
            .memberId(r.getMember().getUserId())
            .classId(c.getClassId())
            .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
            .roomName(c.getRoom() != null ? c.getRoom().getRoomName() : null)
            .coachName(c.getCoach() != null
                ? c.getCoach().getFirstName() + " " + c.getCoach().getLastName() : "Chưa có HLV")
            .coachId(c.getCoach() != null ? c.getCoach().getUserId() : null)
            .coachSpeciality(c.getCoach() != null ? c.getCoach().getSpeciality() : null)
            .startTime(c.getStartTime())
            .endTime(c.getEndTime())
            .status(r.getStatus())
            .registeredAt(r.getRegisteredAt())
            .cancelledAt(r.getCancelledAt())
            .classStatus(c.getStatus())
            .attendanceStatus(a != null && a.getStatus() != null ? a.getStatus().name() : null)
            .attendanceNote(a != null ? a.getNote() : null)
            .build();
    }



}
