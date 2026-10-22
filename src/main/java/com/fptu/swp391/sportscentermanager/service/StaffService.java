package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.StaffCreateRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.StaffResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.StaffUpdateRequestDTO;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;

import java.util.List;

/** BE-25: Manager quản lý nhân viên lễ tân. */
public interface StaffService {
    List<StaffResponseDTO> getReceptionists(String keyword, UserStatus status);
    StaffResponseDTO getReceptionist(Long id);
    StaffResponseDTO createReceptionist(StaffCreateRequestDTO dto);
    StaffResponseDTO updateReceptionist(Long id, StaffUpdateRequestDTO dto);
    StaffResponseDTO toggleReceptionistStatus(Long id);
}
