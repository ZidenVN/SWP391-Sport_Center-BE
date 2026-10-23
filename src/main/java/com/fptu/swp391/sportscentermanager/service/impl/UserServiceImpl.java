package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.RegisterRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.UserResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.UserUpdateRequestDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private UserResponseDTO toResponse(User user) {
        return UserResponseDTO.builder()
            .id(user.getUserId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .gender(user.getGender())
            .email(user.getEmail())
            .phone(user.getPhone())
            .status(user.getStatus() != null ? user.getStatus().name() : null)
            .roleId(user.getRole() != null ? user.getRole().getRoleId() : null)
            .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
            .build();
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public String registerUser(RegisterRequestDTO requestDTO) {

        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }
        if (userRepository.existsByPhone(requestDTO.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_TAKEN);
        }

        Role memberRole = roleRepository.findByRoleName("MEMBER");
        if (memberRole == null) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }

        Member member = Member.builder()
            .firstName(requestDTO.getFirstName())
            .lastName(requestDTO.getLastName())
            .gender(requestDTO.getGender())
            .email(requestDTO.getEmail())
            .phone(requestDTO.getPhone())
            .passwordHash(passwordEncoder.encode(requestDTO.getPassword()))
            .status(UserStatus.ACTIVE)
            .role(memberRole)
            .trainingGoal("Chưa cập nhật")
            .build();

        userRepository.save(member);
        return "Đăng ký tài khoản thành công!";
    }


    // CRUD

    @Override
    public Page<UserResponseDTO> searchUsers(String keyword, String roleName, UserStatus status, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String role = (roleName == null || roleName.isBlank()) ? null : roleName.trim().toUpperCase();
        int safeSize = Math.min(Math.max(size, 1), 100);   // tối đa 100 dòng mỗi trang
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by("userId").descending());
        return userRepository.searchUsers(kw, role, status, pageable).map(this::toResponse);
    }

    @Override
    public UserResponseDTO getUserById(Long id) {
        return toResponse(findUserOrThrow(id));
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(Long id, UserUpdateRequestDTO requestDTO) {
        User user = findUserOrThrow(id);
        if (requestDTO.getEmail() != null && !requestDTO.getEmail().equalsIgnoreCase(user.getEmail())
            && userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }
        if (requestDTO.getPhone() != null && !requestDTO.getPhone().equals(user.getPhone())
            && userRepository.existsByPhone(requestDTO.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_TAKEN);
        }

        if (requestDTO.getFirstName() != null)
            user.setFirstName(requestDTO.getFirstName().trim());
        if (requestDTO.getLastName() != null)
            user.setLastName(requestDTO.getLastName().trim());
        if (requestDTO.getGender() != null)
            user.setGender(requestDTO.getGender().trim().toUpperCase());
        if (requestDTO.getEmail() != null)
            user.setEmail(requestDTO.getEmail().trim());
        if (requestDTO.getPhone() != null)
            user.setPhone(requestDTO.getPhone().trim());

        return toResponse(userRepository.save(user));
    }



    @Override
    @Transactional
    public UserResponseDTO updateUserStatus(Long id, UserStatus newStatus) {
        User user = findUserOrThrow(id);
        user.setStatus(newStatus);
        userRepository.save(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new AppException(ErrorCode.CANNOT_CHANGE_OWN_STATUS);
        }
        User user = findUserOrThrow(id);
        user.setStatus(user.getStatus() == UserStatus.ACTIVE ? UserStatus.INACTIVE : UserStatus.ACTIVE);
        userRepository.save(user);
    }


    @Override
    @Transactional
    public UserResponseDTO assignRoleToUser(Long userId, Long roleId) {
        User user = findUserOrThrow(userId);
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        if (!role.getRoleName().equals(expectedRoleName(user))) {
            throw new AppException(ErrorCode.ROLE_NOT_MATCH_USER_TYPE);
        }

        user.setRole(role);
        return toResponse(user);
    }

    @Override
    @Transactional
    public User changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findUserOrThrow(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash()))
            throw new AppException(ErrorCode.WRONG_CURRENT_PASSWORD);

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.currentTokenVersion() + 1);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        User user = findUserOrThrow(userId);
        user.setTokenVersion(user.currentTokenVersion() + 1);
        userRepository.save(user);
    }

    private String expectedRoleName(User user) {
        if (user instanceof Manager) return "CENTER_MANAGER";
        if (user instanceof Coach) return "COACH";
        if (user instanceof Receptionist) return "RECEPTIONIST";
        if (user instanceof Member) return "MEMBER";
        return null;
    }
}
