package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class SportClassRequestDTO {
    private Integer maxCapacity;
    private LocalDateTime scheduleTime;

    private Long coachId;
    private Long roomId;
    private Long subjectId;
}
