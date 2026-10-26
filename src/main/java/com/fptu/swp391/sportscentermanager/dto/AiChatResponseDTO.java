package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AiChatResponseDTO {
    private Long conversationId;
    private String reply;
}
