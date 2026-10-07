package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseDTO {

    private Long subscriptionId;
    private Long packageId;
    private String packageName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;

    // Thông tin thanh toán (chỉ có khi vừa đăng ký)
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;

}
