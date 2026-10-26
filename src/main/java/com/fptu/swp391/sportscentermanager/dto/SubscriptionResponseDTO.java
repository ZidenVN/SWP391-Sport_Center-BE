package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fptu.swp391.sportscentermanager.enums.PackageType;
import java.util.List;
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
    // Gói tập theo buổi
    private PackageType packageType;
    private boolean wholeCenter;            // FREE không gắn môn = toàn trung tâm (chỉ khu OPEN)
    private List<SubjectAccessDTO> subjects;
    private Integer sessionLimit;           // null = không giới hạn
    private Integer usedSessions;
    private Integer remainingSessions;      // null = không giới hạn hoặc gói FREE
    // Thông tin thanh toán (giao dịch gần nhất của gói)
    // BE-18: FE dùng paymentId để gọi PATCH /api/v1/receptionist/payments/{paymentId}/confirm
    private Long paymentId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;

}
