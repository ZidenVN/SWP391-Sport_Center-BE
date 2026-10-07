package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.entity.Role;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.service.UserService;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String registerUser(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }
        if (userRepository.existsByPhone(user.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_TAKEN);
        }

        user.setStatus("ACTIVE");
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        // (Tùy chọn) Gán role mặc định là MEMBER
        Role memberRole = roleRepository.findByRoleName("MEMBER");
        if (memberRole != null) {
            user.setRole(memberRole);
        }

        userRepository.save(user);
        return "Đăng ký tài khoản thành công!";
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public User updateUser(Long id, User userdetails) {
        User existingUser = getUserById(id);

        existingUser.setFirstName(userdetails.getFirstName());
        existingUser.setLastName(userdetails.getLastName());
        existingUser.setEmail(userdetails.getEmail());
        existingUser.setGender(userdetails.getGender());
        existingUser.setPhone(userdetails.getPhone());

        return existingUser;
    }

    @Override
    public void toggleUserStatus(Long id) {
        User user = getUserById(id);
        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("INACTIVE");
        } else {
            user.setStatus("ACTIVE");
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User assignRoleToUser(Long userId, Long roleId) {
        User user = getUserById(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        user.setRole(role);
        // Nhờ @Transactional, Hibernate sẽ tự động lưu cập nhật.
        return user;
    }
}
