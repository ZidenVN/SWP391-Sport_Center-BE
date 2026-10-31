package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
    private String imageUrl;
    private String coachName;
    private String roomName;
    private String subjectName;

    // BE-15: id để FE lọc/hiển thị (số chỗ: currentEnrolled, availableSeats ở trên - BE-12)
    private Long coachId;
    private Long roomId;
    private Long subjectId;

    // Các trường phục vụ mô hình khoá học & buổi học lặp lịch
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    private String daysOfWeek;
    private LocalTime startClock;
    private LocalTime endClock;
    private LocalDateTime firstSessionAt;
    private Integer totalSessions;
    private String scheduleSummary;
}
