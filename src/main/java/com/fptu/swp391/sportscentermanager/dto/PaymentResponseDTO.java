package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponseDTO {
    private Long paymentId;
    private Long subscriptionId;
    private String packageName;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private String status;
}
