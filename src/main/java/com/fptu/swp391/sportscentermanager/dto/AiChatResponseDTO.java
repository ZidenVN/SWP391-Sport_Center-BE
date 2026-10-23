package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class AiChatResponseDTO {
    // CLAUDE | RULES
    private String source;
    private String reply;
    // Gợi ý câu hỏi tiếp theo cho nút bấm nhanh
    private List<String> suggestions;
}
