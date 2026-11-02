package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.PackageType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private PackageType packageType;
    private Long subjectId;
    private String subjectName;
    private LocalDateTime paidAt;
    private LocalDateTime activatedAt;
    private Long currentClassId;
    private String currentClassName;
    private LocalDate classStartDate;
    private LocalDate classEndDate;

    // Thông tin thanh toán (giao dịch gần nhất của gói)
    // BE-18: FE dùng paymentId để gọi PATCH /api/v1/receptionist/payments/{paymentId}/confirm
    private Long paymentId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;

}
