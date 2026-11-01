package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.PackageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchasePreviewResponseDTO {
    private Long packageId;
    private String packageName;
    private PackageType packageType;
    private String activationNote;
    @Builder.Default
    private List<PurchaseWarningDTO> warnings = new ArrayList<>();
}
