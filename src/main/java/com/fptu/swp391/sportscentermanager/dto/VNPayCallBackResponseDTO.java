package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VNPayCallBackResponseDTO {
    private Boolean success;
    private String message;
    private Long paymentId;
    private String transactionNo;
    private String responseCode;
}
