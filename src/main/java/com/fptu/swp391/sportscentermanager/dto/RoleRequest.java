package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class RoleRequest {
    @NotBlank
    private String roleName;

    @Size(max = 500)
    private String description;

    @NotNull
    private List<Long> permissionIds;
}
