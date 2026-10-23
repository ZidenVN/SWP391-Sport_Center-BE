package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/** HLV yêu cầu AI gợi ý bài tập cho 1 học viên. Bỏ trống thì AI tự suy từ mục tiêu + lịch sử tập. */
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class AiSuggestionRequestDTO {
    @Size(max = 200, message = "Trọng tâm tối đa 200 ký tự")
    private String focus;

    @Pattern(regexp = "^(BEGINNER|INTERMEDIATE|ADVANCED)?$", message = "Trình độ không hợp lệ")
    private String level;

    @Min(value = 20, message = "Buổi tập tối thiểu 20 phút")
    @Max(value = 120, message = "Buổi tập tối đa 120 phút")
    private Integer sessionMinutes;
}
