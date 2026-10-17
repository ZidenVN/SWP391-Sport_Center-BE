package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutResultResponseDTO {
    private Long resultId;
    private Long memberId;
    private String memberName;
    private String activityDetails;
    private String coachFeedback;
    private LocalDateTime loggedDate;
}
