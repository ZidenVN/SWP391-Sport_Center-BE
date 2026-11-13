package com.fptu.swp391.sportscentermanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Lớp mà 1 gói lớp đăng ký / đổi sang được
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class EligibleClassResponseDTO {
    private Long classId;
    private String classCode;   // mã lớp hiển thị, vd YOG-0012
    private String subjectName;
    private String coachName;
    private String roomName;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    private String daysOfWeek;
    private LocalTime startClock;
    private LocalTime endClock;
    private LocalDateTime firstSessionAt;
    private Integer maxCapacity;
    private Long registeredCount;
}
