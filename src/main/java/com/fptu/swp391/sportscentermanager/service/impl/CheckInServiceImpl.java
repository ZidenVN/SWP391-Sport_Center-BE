package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.CheckInResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.CheckInRepository;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.ReceptionistRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
import com.fptu.swp391.sportscentermanager.service.CheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CheckInRepository checkInRepository;
    private final ReceptionistRepository receptionistRepository;

    @Override
    @Transactional
    public CheckInResponseDTO checkIn(Long memberId, Long receptionistId) {
        Member member = memberRepository.findByIdForUpdate(memberId).orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.MEMBER_INACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        Subscription current = subscriptionRepository.findCurrent(memberId, now).stream().findFirst().orElseThrow(() -> new AppException((ErrorCode.NO_ACTIVE_SUBSCRIPTION)));

        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        if (checkInRepository.existsByMember_UserIdAndCheckInTimeBetween(memberId, startOfDay, startOfDay.plusDays(1))) {
            throw new AppException(ErrorCode.ALREADY_CHECK_IN);
        }
        CheckIn saved = checkInRepository.save(CheckIn.builder()
                .member(member)
                .receptionist(receptionistRepository.getReferenceById(receptionistId))
                .checkInTime(now)
            .build());
        CheckInResponseDTO dto = toResponse(saved);
        dto.setPackageName(current.getMembershipPackage().getPackageName());
        dto.setSubscriptionEndDate(current.getEndDate());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInResponseDTO> getCheckIns(LocalDate date){
        LocalDateTime from = date.atStartOfDay();
        return checkInRepository.findInRange(from, from.plusDays(1)).stream().map(this::toResponse).toList();
    }

    @Override
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

        subscriptionRepository.findCurrent(m.getUserId(), ci.getCheckInTime())
            .stream().findFirst().ifPresent(sub -> {
                dto.setPackageName(sub.getMembershipPackage().getPackageName());
                dto.setSubscriptionEndDate(sub.getEndDate());
            });

        return dto;
    }
}
