package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class AiSuggestionResponseDTO {
    // CLAUDE: do Claude tạo | RULES: bộ gợi ý dựng sẵn (chưa cấu hình API key hoặc Claude lỗi)
    private String source;
    private String model;
    private Long memberId;
    private String memberName;
    private String goal;
    private String level;
    private Integer sessionMinutes;
    // Dữ liệu AI đã dựa vào (để HLV kiểm tra lại)
    private List<String> basedOn;
    private AiExercisePlan plan;
}
