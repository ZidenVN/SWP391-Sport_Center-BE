package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.MemberAttendanceResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MemberProfileResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MemberProfileUpdateDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.ClassAttendanceRepository;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.service.MemberService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

//TODO: Tìm hiểu lý do vì sao lại phải có @Service?
@Service
//TODO: Tìm hiểu tác dụng của RequiredArgsConstructor
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    //TODO: Service gọi repo là đúng rồi nhưng tại sao lại là final?
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ClassAttendanceRepository classAttendanceRepository;

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private MemberProfileResponseDTO toResponse(Member m){
        return MemberProfileResponseDTO.builder()
            .memberId(m.getUserId())
            .firstName(m.getFirstName())
            .lastName(m.getLastName())
            .gender(m.getGender())
            .email(m.getEmail())
            .phone(m.getPhone())
            .trainingGoal(m.getTrainingGoal())
            .build();
    }

    @Override
    public MemberProfileResponseDTO getProfile(Long memberId) {
        return toResponse(findMember(memberId));
    }

    @Override
    @Transactional
    public MemberProfileResponseDTO updateProfile(Long memberId, MemberProfileUpdateDTO dto) {
        Member member = findMember(memberId);

        // Chỉ kiểm tra trùng SĐT khi người dùng đổi sang số khác
        if (!Objects.equals(member.getPhone(), dto.getPhone())
            && userRepository.existsByPhone(dto.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_TAKEN);
        }

        member.setFirstName(dto.getFirstName());
        member.setLastName(dto.getLastName());
        member.setGender(dto.getGender());
        member.setPhone(dto.getPhone());
        member.setTrainingGoal(dto.getTrainingGoal());

        return toResponse(memberRepository.save(member));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberAttendanceResponseDTO> getMyAttendances(Long memberId) {
        return classAttendanceRepository.findHistoryByMember(memberId).stream().map(a -> {
            SportClass c = a.getSportClass();
            return MemberAttendanceResponseDTO.builder()
                .attendanceId(a.getAttendanceId())
                .classId(c.getClassId())
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                .coachName(c.getCoach() != null ? c.getCoach().getLastName() + " " + c.getCoach().getFirstName() : null)
                .roomName(c.getRoom() !=  null ? c.getRoom().getRoomName() : null)
                .startTime(c.getStartTime())
                .endTime(c.getEndTime())
                .status(a.getStatus())
                .statusDescription(a.getStatus() != null ? a.getStatus().getDescription() :  null)
                .note(a.getNote())
                .markedAt(a.getMarkedAt())
                .build();
        }).toList();
    }

}
