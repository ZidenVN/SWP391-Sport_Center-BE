package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutPlanRequestDTO {
    @NotBlank(message = "Tên giáo án không được trống")
    private String planName;

    @NotBlank(message = "Nội dung bài tập không được để trống")
    private String exerciseDetails;

    private String duration;

    private LocalDate dueDate;

    @NotNull(message = "Loại đối tượng áp dụng bắt buộc (INDIVIDUAL hoặc CLASS)")
    private String targetType;

    @NotNull(message = "ID đối tượng áp dụng là bắt buộc")
    private Long targetId;
}
