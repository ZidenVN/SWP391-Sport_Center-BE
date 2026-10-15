package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.WorkoutPlanRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.WorkoutPlanResponseDTO;

import java.util.List;

public interface WorkoutPlanService {
    WorkoutPlanResponseDTO createAndAssignWorkoutPlan(Long coachId, WorkoutPlanRequestDTO requestDTO);

    List<WorkoutPlanResponseDTO> getPlansByCoach(Long coachId);

    List<WorkoutPlanResponseDTO> getPlansByMember(Long memberId);

    List<WorkoutPlanResponseDTO> getPlansByClass(Long classId);
}
