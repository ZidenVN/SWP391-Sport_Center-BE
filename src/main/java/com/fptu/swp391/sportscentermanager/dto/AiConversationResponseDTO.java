package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Setter @Getter
@Builder @NoArgsConstructor @AllArgsConstructor
public class AiConversationResponseDTO {
    private Long conversationId;
    private String title;
    private LocalDateTime updatedAt;
}
