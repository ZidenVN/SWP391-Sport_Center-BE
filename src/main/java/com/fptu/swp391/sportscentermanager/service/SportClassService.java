package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.SportClassRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SportClassResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.SportClass;

import java.util.List;

public interface SportClassService {
    SportClassResponseDTO createClass(SportClassRequestDTO requestDTO);
    SportClass getClassById(Long id);
    SportClassResponseDTO getSportClassById(Long id);
    List<SportClassResponseDTO> getAllClasses();
    SportClassResponseDTO assignCoachToClass(Long classId, Long coachId);
    SportClassResponseDTO updateClass(Long id, SportClassRequestDTO requestDTO);
    void deleteClass(Long id);
}
