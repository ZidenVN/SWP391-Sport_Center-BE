package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.SportClassRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SportClassResponseDTO;

import java.util.List;

public interface SportClassService {
    SportClassResponseDTO createClass(SportClassRequestDTO requestDTO);
    List<SportClassResponseDTO> getAllClasses();
}
