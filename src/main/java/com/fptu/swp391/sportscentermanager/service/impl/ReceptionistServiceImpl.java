package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.MemberSearchResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.WalkInMemberRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.WalkInMemberResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.Role;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.service.ReceptionistService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceptionistServiceImpl implements ReceptionistService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Override
    @Transactional
    public WalkInMemberResponseDTO registerWalkInMember(WalkInMemberRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }
        if (userRepository.existsByPhone(dto.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_TAKEN);
        }
        Role memberRole = roleRepository.findByRoleName("MEMBER");
        if (memberRole == null) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }

        String rawPassword = generateTemporaryPassword();
        String goal = (dto.getTrainingGoal() == null || dto.getTrainingGoal().isBlank())
            ? "Chưa cập nhật" : dto.getTrainingGoal();

        Member member = memberRepository.save(Member.builder()
            .firstName(dto.getFirstName())
            .lastName(dto.getLastName())
            .gender(dto.getGender())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .passwordHash(passwordEncoder.encode(rawPassword))
            .status("ACTIVE")
            .role(memberRole)
            .trainingGoal(goal)
            .build());

        return WalkInMemberResponseDTO.builder()
            .member(toResponse(member))
            .temporaryPassword(rawPassword)
            .build();
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
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
