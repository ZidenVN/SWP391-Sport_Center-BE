package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class PostponeSessionRequestDTO {
    @NotBlank(message = "Lý do hoãn buổi học không được để trống")
    private String reason;
}
