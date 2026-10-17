package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberProgressResponseDTO {
    private Long memberId;
    private String memberName;
    private String trainingGoal;

    private long totalSessionAttended;
    private int totalPlansAssigned;
    private int completedPlans;
    private int inProgressPlans;

    private String lastestCoachFeedback;
    private List<WorkoutResultResponseDTO> recentWorkoutResults;
}
