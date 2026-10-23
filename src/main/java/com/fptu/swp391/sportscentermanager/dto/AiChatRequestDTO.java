package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class AiChatRequestDTO {
    @NotBlank(message = "Vui lòng nhập câu hỏi")
    @Size(max = 1000, message = "Câu hỏi tối đa 1000 ký tự")
    private String message;

    // Các lượt trước trong cuộc trò chuyện (FE giữ), tối đa 20 lượt gần nhất
    @Valid
    @Size(max = 20, message = "Lịch sử trò chuyện quá dài")
    private List<Turn> history;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class Turn {
        @Pattern(regexp = "^(user|assistant)$", message = "Vai trò không hợp lệ")
        private String role;
        @Size(max = 4000)
        private String content;
    }
}
