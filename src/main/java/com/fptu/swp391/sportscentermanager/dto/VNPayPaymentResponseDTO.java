package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VNPayPaymentResponseDTO {
    private String paymentUrl;
    private Long paymentId;
    private Long subscriptionId;
}
