package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.PackageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

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

    // BR-PKG-04: mọi gói đều có hạn dùng
    @NotNull
    @Positive
    private Integer durationDays;

    @NotNull
    private PackageType packageType;

    // BR-PKG-05: null = không giới hạn (CLASS). FREE phải để null
    @Positive
    private Integer sessionLimit;

    // CLASS: ít nhất 1 môn. FREE: 1 môn OPEN hoặc để trống = toàn trung tâm
    @Builder.Default
    private Set<Long> subjectIds = new HashSet<>();

    // ACTIVE | INACTIVE, bỏ trống = ACTIVE
    private String status;
}
