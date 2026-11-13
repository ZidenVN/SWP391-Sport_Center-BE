package com.fptu.swp391.sportscentermanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRegistrationResponseDTO {
    private Long registrationId;
    private Long memberId;
    private Long classId;
    private String classCode;   // mã lớp hiển thị, vd YOG-0012
    private String subjectName;
    private String roomName;
    private String coachName;
    private String imageUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    // Lịch cố định của lớp
    private LocalDate startDate;
    private LocalDate endDate;
    private String daysOfWeek;
    private LocalTime startClock;
    private LocalTime endClock;
    private LocalDateTime firstSessionAt;
    private String classStatus;
    private String status;           // REGISTERED | SWITCHED | CANCELLED | STOPPED
    private LocalDateTime registeredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime switchedAt;
    // Gói lớp dùng cho lần đăng ký này
    private Long subscriptionId;
    private String packageName;
    private Integer receptionistSwitchCount;
}
