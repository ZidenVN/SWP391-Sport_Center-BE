package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class SportClassResponseDTO {
    private Long classId;
    private Integer maxCapacity;
    private LocalDateTime scheduleTime;
    private String status;
    private String coachName;
    private String roomName;
    private String subjectName;
}
