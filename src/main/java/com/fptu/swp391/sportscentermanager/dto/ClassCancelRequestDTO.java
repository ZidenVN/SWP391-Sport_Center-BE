package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class ClassCancelRequestDTO {
    // Lý do hủy, sẽ gửi kèm thông báo cho học viên (không bắt buộc)
    @Size(max = 500)
    private String reason;
}
