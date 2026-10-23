package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.RegisterRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.UserResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.UserUpdateRequestDTO;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {
    String registerUser(RegisterRequestDTO requestDTO);

   Page<UserResponseDTO> searchUsers(String keyword, String roleName, UserStatus status, int page, int size);


    UserResponseDTO getUserById(Long id);
    UserResponseDTO updateUser(Long id, UserUpdateRequestDTO requestDTO);

    UserResponseDTO updateUserStatus(Long id, UserStatus newStatus);
    void toggleUserStatus(Long id, Long currentUserId);
    UserResponseDTO assignRoleToUser(Long userId, Long roleId);

    User changePassword(Long userId, String currentPassword, String newPassword);

    void logout(Long userId);

}
