package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
public class QuizStatusResponseDTO {
    private boolean completed;
    private LocalDateTime completedAt;
}
