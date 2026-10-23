package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.CheckInResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.CheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final MemberCheckInRepository checkInRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ClassRegistrationRepository registrationRepository;

    // Lễ tân quẹt thẻ / tìm hội viên khi khách đến: phải có gói còn hiệu lực, mỗi ngày tính 1 lượt
    @Override
    @Transactional
    public CheckInResponseDTO checkIn(Long memberId, Long staffId, String note) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() == UserStatus.INACTIVE) {
            throw new AppException(ErrorCode.MEMBER_ACCOUNT_LOCKED);
        }

        LocalDateTime now = LocalDateTime.now();
        Subscription current = subscriptionRepository.findCurrent(memberId, now).stream().findFirst()
            .orElseThrow(() -> new AppException(ErrorCode.NO_ACTIVE_SUBSCRIPTION));

        LocalDate today = now.toLocalDate();
        if (checkInRepository.existsByMember_UserIdAndCheckInTimeBetween(memberId, today.atStartOfDay(), today.atTime(LocalTime.MAX))) {
            throw new AppException(ErrorCode.ALREADY_CHECK_IN);
        }

        User staff = staffId != null ? userRepository.findById(staffId).orElse(null) : null;
        MemberCheckIn saved = checkInRepository.save(MemberCheckIn.builder()
            .member(member)
            .checkedInBy(staff)
            .checkInTime(now)
            .note(note != null && !note.isBlank() ? note.trim() : null)
            .build());
        return toResponse(saved, current, todayClasses(memberId, today));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInResponseDTO> getByDate(LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now();
        return checkInRepository.findInRange(day.atStartOfDay(), day.atTime(LocalTime.MAX)).stream()
            .map(c -> toResponse(c, null, null))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInResponseDTO> getMemberHistory(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return checkInRepository.findByMember_UserIdOrderByCheckInTimeDesc(memberId).stream()
            .map(c -> toResponse(c, null, null))
            .toList();
    }

    // Lớp hội viên đã đăng ký trong ngày, để lễ tân chỉ phòng tập
    private List<CheckInResponseDTO.TodayClass> todayClasses(Long memberId, LocalDate today) {
        return registrationRepository.findHistory(memberId).stream()
            .filter(r -> "REGISTERED".equals(r.getStatus()))
            .map(ClassRegistration::getSportClass)
            .filter(c -> c.getStartTime() != null && c.getStartTime().toLocalDate().equals(today))
            .filter(c -> !"CANCELLED".equals(c.getStatus()))
            .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
            .map(c -> CheckInResponseDTO.TodayClass.builder()
                .classId(c.getClassId())
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                .roomName(c.getRoom() != null ? c.getRoom().getRoomName() : null)
                .coachName(c.getCoach() != null ? c.getCoach().getFirstName() + " " + c.getCoach().getLastName() : null)
                .startTime(c.getStartTime())
                .endTime(c.getEndTime())
                .build())
            .toList();
    }

    private CheckInResponseDTO toResponse(MemberCheckIn c, Subscription sub, List<CheckInResponseDTO.TodayClass> classes) {
        Member m = c.getMember();
        User by = c.getCheckedInBy();
        CheckInResponseDTO.CheckInResponseDTOBuilder b = CheckInResponseDTO.builder()
            .checkInId(c.getCheckInId())
            .memberId(m.getUserId())
            .memberName(m.getFirstName() + " " + m.getLastName())
            .phone(m.getPhone())
            .checkInTime(c.getCheckInTime())
            .checkedInBy(by != null ? by.getFirstName() + " " + by.getLastName() : null)
            .note(c.getNote())
            .todayClasses(classes);
        if (sub != null) {
            b.packageName(sub.getMembershipPackage() != null ? sub.getMembershipPackage().getPackageName() : null)
                .packageEndDate(sub.getEndDate())
                .daysLeft(Math.max(0, Duration.between(c.getCheckInTime(), sub.getEndDate()).toDays()));
        }
        return b.build();
    }
}
