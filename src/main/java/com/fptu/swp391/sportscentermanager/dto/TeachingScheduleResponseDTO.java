package com.fptu.swp391.sportscentermanager.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
public class TeachingScheduleResponseDTO {
    private Long classId;
    private String subjectName;
    private String roomName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxCapacity;
    private Long currentEnrolled;
    private String status;
    private String imageUrl;
}

