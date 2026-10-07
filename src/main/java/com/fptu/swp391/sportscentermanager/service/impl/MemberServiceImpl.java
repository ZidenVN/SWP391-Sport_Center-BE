package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.MemberProfileResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MemberProfileUpdateDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.service.MemberService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

//TODO: Tìm hiểu lý do vì sao lại phải có @Service?
@Service
//TODO: Tìm hiểu tác dụng của RequiredArgsConstructor
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    //TODO: Service gọi repo là đúng rồi nhưng tại sao lại là final?
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

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
        if (!member.getPhone().equals(dto.getPhone())
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

}
