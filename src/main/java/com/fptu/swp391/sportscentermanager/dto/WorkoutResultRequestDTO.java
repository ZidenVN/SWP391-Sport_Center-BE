package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutResultRequestDTO {
    @NotNull(message = "ID học viên không được để trống")
    private Long memberId;

    @NotBlank(message = "Chi tiết bài tập/kết quả không được để trống")
    private String activityDetails;

    private String coachFeedback;

    private LocalDateTime loggedDate;
}
