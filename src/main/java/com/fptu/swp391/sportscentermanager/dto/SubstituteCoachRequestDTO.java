package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class SubstituteCoachRequestDTO {
    @NotNull(message = "ID của huấn luyện viên thay thế không được để trống")
    private Long coachId;

    private String note;
}
