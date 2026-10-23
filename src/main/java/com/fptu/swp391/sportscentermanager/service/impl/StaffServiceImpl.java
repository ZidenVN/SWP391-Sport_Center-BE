package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.StaffCreateRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.StaffResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.StaffUpdateRequestDTO;
import com.fptu.swp391.sportscentermanager.entity.Receptionist;
import com.fptu.swp391.sportscentermanager.entity.Role;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.ReceptionistRepository;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final ReceptionistRepository receptionistRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponseDTO> getReceptionists(String keyword, UserStatus status) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return receptionistRepository.search(kw, status).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponseDTO getReceptionist(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public StaffResponseDTO createReceptionist(StaffCreateRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }
        if (userRepository.existsByPhone(dto.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_TAKEN);
        }
        // Tự gán role RECEPTIONIST, không cho client chọn role
        Role role = roleRepository.findByRoleName("RECEPTIONIST");
        if (role == null) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }

        Receptionist saved = receptionistRepository.save(Receptionist.builder()
            .firstName(dto.getFirstName())
            .lastName(dto.getLastName())
            .gender(dto.getGender())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .passwordHash(passwordEncoder.encode(dto.getPassword()))
            .status(UserStatus.ACTIVE)
            .role(role)
            .build());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public StaffResponseDTO updateReceptionist(Long id, StaffUpdateRequestDTO dto) {
        Receptionist r = findOrThrow(id);

        if (!dto.getEmail().equalsIgnoreCase(r.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }
        if (!dto.getPhone().equals(r.getPhone()) && userRepository.existsByPhone(dto.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_TAKEN);
        }

        r.setFirstName(dto.getFirstName());
        r.setLastName(dto.getLastName());
        r.setGender(dto.getGender());
        r.setEmail(dto.getEmail());
        r.setPhone(dto.getPhone());
        return toResponse(receptionistRepository.save(r));
    }

    @Override
    @Transactional
    public StaffResponseDTO toggleReceptionistStatus(Long id) {
        Receptionist r = findOrThrow(id);
        // Khóa thì JwtAuthFilter sẽ chặn token cũ, mở lại vẫn giữ nguyên lịch sử dữ liệu
        r.setStatus(r.getStatus() == UserStatus.ACTIVE ? UserStatus.INACTIVE : UserStatus.ACTIVE);
        return toResponse(receptionistRepository.save(r));
    }

    private Receptionist findOrThrow(Long id) {
        return receptionistRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.RECEPTIONIST_NOT_FOUND));
    }

    private StaffResponseDTO toResponse(Receptionist r) {
        return StaffResponseDTO.builder()
            .userId(r.getUserId())
            .firstName(r.getFirstName())
            .lastName(r.getLastName())
            .gender(r.getGender())
            .email(r.getEmail())
            .phone(r.getPhone())
            .status(r.getStatus() != null ? r.getStatus().name() : null)
            .roleName(r.getRole() != null ? r.getRole().getRoleName() : null)
            .createdAt(r.getCreatedAt())
            .build();
    }
}
