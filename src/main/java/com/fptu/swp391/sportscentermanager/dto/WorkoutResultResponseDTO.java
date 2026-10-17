package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class WorkoutResultResponseDTO {
    private Long resultId;
    private Long memberId;
    private String memberName;
    private String activityDetails;
    private String coachFeedback;
    private LocalDateTime loggedDate;
}
