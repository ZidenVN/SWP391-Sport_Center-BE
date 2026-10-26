package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.PackageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Kết quả xem trước khi mua gói: ngày dự kiến (BR-SUB-05) + cảnh báo gói thừa (BR-SUB-08)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchasePreviewResponseDTO {
    private Long packageId;
    private String packageName;
    private PackageType packageType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @Builder.Default
    private List<PurchaseWarningDTO> warnings = new ArrayList<>();
}
