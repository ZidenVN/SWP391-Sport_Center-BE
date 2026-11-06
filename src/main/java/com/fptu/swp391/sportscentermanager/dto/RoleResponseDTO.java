package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleResponseDTO {
    private Long roleId;
    private String roleName;
    private String description;
}
