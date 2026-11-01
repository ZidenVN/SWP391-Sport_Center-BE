package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class ClassStopRequestDTO {
    @NotBlank(message = "Lý do dừng lớp khẩn cấp không được để trống")
    private String reason;
}
