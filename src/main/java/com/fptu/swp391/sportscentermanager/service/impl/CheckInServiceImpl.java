package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.CheckInResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.CheckInType;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.PackageType;
import com.fptu.swp391.sportscentermanager.enums.SubscriptionStatus;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.CheckInService;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import java.time.format.DateTimeFormatter;
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CheckInRepository checkInRepository;
    private final ReceptionistRepository receptionistRepository;
    private final SubscriptionService subscriptionService;
    private final ClassSessionRepository classSessionRepository;
    private final NotificationService notificationService;
    @Value("${app.checkin.class-open-minutes-before:30}")
    private long classOpenMinutesBefore;

    @Override
    @Transactional
    public CheckInResponseDTO checkIn(Long memberId, Long receptionistId) {
        Member member = memberRepository.findByIdForUpdate(memberId).orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.MEMBER_INACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        if (checkInRepository.existsByMember_UserIdAndCheckInTimeBetween(memberId, startOfDay, startOfDay.plusDays(1))) {
            throw new AppException(ErrorCode.ALREADY_CHECK_IN);
        }

        // membership đang chờ active thì active luôn

        subscriptionService.activateMembershipIfWaiting(memberId, now);

        List<Subscription> current = subscriptionRepository.findCurrent(memberId, now);
        Optional<Subscription> membership = current.stream().filter(s -> s.getMembershipPackage().getPackageType() == PackageType.MEMBERSHIP).findFirst();

        CheckIn checkIn = CheckIn.builder().member(member).receptionist(receptionistRepository.getReferenceById(receptionistId))
            .checkInTime(now)
            .type(CheckInType.FACILITY)
            .build();
        Subscription shown;

        if (membership.isPresent()) {
            shown = membership.get();
        } else {
            Optional<Subscription> classPackage = current.stream().filter(s -> s.getMembershipPackage().getPackageType() == PackageType.CLASS).findFirst();
            ClassSession session = classSessionRepository.findMemberSessionAround(memberId, now, now.plusMinutes(classOpenMinutesBefore)).stream().findFirst().orElseThrow(() -> new AppException(classPackage.isPresent() ? ErrorCode.CHECKIN_OUTSIDE_CLASS_TIME : ErrorCode.NO_ACTIVE_SUBSCRIPTION));
            checkIn.setType(CheckInType.CLASS);
            checkIn.setSession(session);
            shown = classPackage.orElse(null);
        }

        CheckIn saved = checkInRepository.save(checkIn);
        CheckInResponseDTO dto = toResponse(saved);
        if (shown != null) {
            dto.setPackageName(shown.getMembershipPackage().getPackageName());
            dto.setSubscriptionEndDate(shown.getEndDate());
        }

        // getCurrentSubscriptions trả kèm gói chờ kích hoạt; lễ tân chỉ cần gói đang dùng được
        dto.setActiveSubscriptions(
            subscriptionService.getCurrentSubscriptions(memberId).stream()
                .filter(s ->
                    SubscriptionStatus.ACTIVE.name().equals(s.getStatus())
                )
                .toList()
        );

        notificationService.notifyUsers(
            List.of(member),
            "Check-in thành công",
            "Bạn đã check-in tại trung tâm lúc "
                + now.format(
                DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
            ),
            "INDIVIDUAL"
        );

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInResponseDTO> getCheckIns(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        return checkInRepository.findInRange(from, from.plusDays(1)).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInResponseDTO> getMemberCheckIns(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return checkInRepository.findByMemberId(memberId).stream().map(this::toResponse).toList();
    }

    private CheckInResponseDTO toResponse(CheckIn ci) {
        Member m = ci.getMember();
        Receptionist r = ci.getReceptionist();
        CheckInResponseDTO dto = CheckInResponseDTO.builder()
            .checkInId(ci.getCheckInId())
            .memberId(m.getUserId())
            .memberName(m.getLastName() + " " + m.getFirstName())
            .phone(m.getPhone())
            .checkInTime(ci.getCheckInTime())
            .receptionistName(r != null ? r.getLastName() + " " + r.getFirstName() : null)
            .build();

        dto.setCheckInType(ci.getType());
        if (ci.getSession() != null) {
            ClassSession s = ci.getSession();
            dto.setClassSessionId(s.getSessionId());

            dto.setClassName(s.getSportClass().displayName());

            dto.setSessionStartTime(s.getStartTime());
        }

        subscriptionRepository.findCurrent(m.getUserId(), ci.getCheckInTime())
            .stream().findFirst().ifPresent(sub -> {
                dto.setPackageName(sub.getMembershipPackage().getPackageName());
                dto.setSubscriptionEndDate(sub.getEndDate());
            });

        return dto;
    }
}
