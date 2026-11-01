package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.PackageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPackageRequestDTO {

    @NotBlank
    private String packageName;

    private String description;

    @NotNull
    @PositiveOrZero
    private Double price;

    private Long subjectId;

    // BR-PKG-04: mọi gói đều có hạn dùng
    @NotNull
    @Positive
    private Integer durationDays;

    @NotNull
    private PackageType packageType;

    // ACTIVE | INACTIVE, bỏ trống = ACTIVE
    private String status;
}
