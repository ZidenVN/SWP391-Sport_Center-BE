package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.PackageType;
import lombok.*;

import java.time.LocalDateTime;

// BR-SUB-08: một gói đang có đã cấp quyền vào cùng khu với gói định mua
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseWarningDTO {
    private Long subscriptionId;
    private String packageName;
    private PackageType packageType;
    private LocalDateTime endDate;
    private long daysLeft;
    private Integer remainingSessions; // null = không giới hạn hoặc gói FREE
    private String message;
}
