package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageResponseDTO {
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
