package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** BE-18/24: hủy giao dịch chờ hoặc hoàn tiền, bắt buộc ghi lý do để lưu vết. */
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentActionRequestDTO {
    @NotBlank(message = "Lý do không được để trống")
    @Size(max = 500)
    private String reason;
}
