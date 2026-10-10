package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.MemberSearchResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.service.ReceptionistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceptionistServiceImpl implements ReceptionistService {

    private final MemberRepository memberRepository;

    @Override
    public List<MemberSearchResponseDTO> searchMembers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return memberRepository.search(keyword.trim())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public MemberSearchResponseDTO getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .map(this::toResponse)
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private MemberSearchResponseDTO toResponse(Member m) {
        return MemberSearchResponseDTO.builder()
            .memberId(m.getUserId())
            .firstName(m.getFirstName())
            .lastName(m.getLastName())
            .gender(m.getGender())
            .email(m.getEmail())
            .phone(m.getPhone())
            .status(m.getStatus())
            .trainingGoal(m.getTrainingGoal())
            .build();
    }
}
