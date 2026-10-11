package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dữ liệu hóa đơn. Backend trả JSON, Front-end chịu trách nhiệm in / xuất PDF. */
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class InvoiceResponseDTO {
    private String invoiceNumber;      // ví dụ INV-000012
    private Long paymentId;
    private LocalDateTime issuedAt;

    // Khách hàng
    private Long memberId;
    private String memberName;
    private String memberEmail;
    private String memberPhone;

    // Gói tập
    private Long subscriptionId;
    private String packageName;
    private Integer durationDays;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Thanh toán
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
}
