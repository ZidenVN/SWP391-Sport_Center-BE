package com.fptu.swp391.sportscentermanager.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
public class TeachingScheduleResponseDTO {
    private Long sessionId;            // buổi học cụ thể
    private Long classId;
    private String subjectName;
    private String roomName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxCapacity;
    private Long currentEnrolled;
    private String status;             // trạng thái BUỔI: SCHEDULED/POSTPONED/DONE/CANCELLED
    private String classStatus;        // trạng thái LỚP
    private String type;               // REGULAR / MAKEUP
    private Boolean substitute;        // true nếu mình đang dạy thay
    private String mainCoachName;      // HLV chính của lớp
    private String imageUrl;
}

