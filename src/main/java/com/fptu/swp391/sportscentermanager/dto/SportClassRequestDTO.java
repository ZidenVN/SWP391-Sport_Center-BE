package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class SportClassRequestDTO {
    @NotNull
    @Min(1)
    private Integer maxCapacity;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    @Future(message = "Thời gian học phải ở trong tương lai")
    private LocalDateTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    @Future(message = "Giờ kết thúc phải ở trong tương lai")
    private LocalDateTime endTime;

    @NotNull
    private Long coachId;

    @NotNull
    private Long roomId;

    @NotNull
    private Long subjectId;
}
