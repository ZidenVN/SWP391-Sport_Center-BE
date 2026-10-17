package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.entity.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String gender;
    private String email;
    private String phone;
    private String status;
    private Long roleId;
    private String roleName;
}
