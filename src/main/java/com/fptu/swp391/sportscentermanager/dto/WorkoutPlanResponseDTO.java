package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Setter
@AllArgsConstructor
@Builder
public class WorkoutPlanResponseDTO {
    private Long planId;
    private String planName;
    private String exerciseDetails;
    private String duration;
    private Long coachId;
    private String coachName;
    private String targetType;
    private Long targetId;
    private String targetName;
    private LocalDate assignedDate;
    private LocalDate dueDate;
    private String status;
    private LocalDateTime createdAt;
}
