package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.WorkoutResultRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.WorkoutResultResponseDTO;

import java.util.List;

public interface WorkoutResultService {
    WorkoutResultResponseDTO recordWorkoutResult(WorkoutResultRequestDTO requestDTO);

    List<WorkoutResultResponseDTO> getResultsByMember(Long memberId);
}
