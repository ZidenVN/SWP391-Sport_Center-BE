package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/** BE-18: lễ tân xác nhận đã thu tiền tại quầy. */
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class ConfirmPaymentRequestDTO {
    // Bắt buộc với BANK_TRANSFER (mã giao dịch ngân hàng), không bắt buộc với CASH
    @Size(max = 100)
    private String transactionRef;

    @Size(max = 500)
    private String note;
}
