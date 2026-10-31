package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class SportClassRequestDTO {
    @NotNull(message = "Sĩ số tối đa không được để trống")
    @Min(value = 1, message = "Sĩ số tối đa phải lớn hơn 0")
    private Integer maxCapacity;

    // Các trường mới dành cho mô hình khoá học lặp lịch cố định
    private LocalDate startDate;
    private Integer durationDays; // 30, 60, 90 ngày
    private String daysOfWeek;   // ví dụ: "MON,WED,FRI"
    private LocalTime startClock; // ví dụ: 08:00
    private LocalTime endClock;   // ví dụ: 09:30

    // Giữ lại để tương thích ngược nếu payload cũ gửi startTime / endTime
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @NotNull(message = "Huấn luyện viên không được để trống")
    private Long coachId;

    @NotNull(message = "Phòng học không được để trống")
    private Long roomId;

    @NotNull(message = "Môn học không được để trống")
    private Long subjectId;
}
