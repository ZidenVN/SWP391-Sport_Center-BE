package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.MyProfileResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MyProfileUpdateDTO;
import com.fptu.swp391.sportscentermanager.dto.RegisterRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.UserResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;

import java.util.List;

public interface UserService {
    String registerUser(RegisterRequestDTO requestDTO);

    List<UserResponseDTO> getAllUsers();
    UserResponseDTO getUserById(Long id);
    UserResponseDTO updateUser(Long id, UserResponseDTO requestDTO);

    UserResponseDTO updateUserStatus(Long id, UserStatus newStatus);
    void toggleUserStatus(Long id);
    UserResponseDTO assignRoleToUser(Long userId, Long roleId);

    // Hồ sơ của chính người đăng nhập (mọi vai trò)
    MyProfileResponseDTO getMyProfile(Long userId, List<String> permissions);
    MyProfileResponseDTO updateMyProfile(Long userId, MyProfileUpdateDTO dto, List<String> permissions);

}
