package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class MakeupSessionRequestDTO {
    @NotNull(message = "ID của buổi học bị hoãn không được để trống")
    private Long postponedSessionId;

    @NotNull(message = "Thời gian bắt đầu buổi dạy bù không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc buổi dạy bù không được để trống")
    private LocalDateTime endTime;

    private Long roomId;

    private Long coachId;

    private String note;
}
