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

    @NotNull
    @Future(message = "Thời gian học phải ở trong tương lai")
    private LocalDateTime scheduleTime;

    @NotNull
    private Long coachId;

    @NotNull
    private Long roomId;

    @NotNull
    private Long subjectId;
}
