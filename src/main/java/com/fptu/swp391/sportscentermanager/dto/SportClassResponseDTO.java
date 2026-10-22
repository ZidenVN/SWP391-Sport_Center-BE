package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class SportClassResponseDTO {
    private Long classId;
    private Integer maxCapacity;
    private Long currentEnrolled;
    private Long availableSeats;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String coachName;
    private String roomName;
    private String subjectName;
}
