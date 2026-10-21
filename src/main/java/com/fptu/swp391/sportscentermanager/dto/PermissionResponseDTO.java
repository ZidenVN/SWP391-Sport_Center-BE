package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponseDTO {
    private Long permissionId;
    private String permissionName;
    private String description;
}
