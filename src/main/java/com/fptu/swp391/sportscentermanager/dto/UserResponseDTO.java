package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// @NoArgsConstructor cần cho Jackson vì DTO này cũng là request body của PUT /users/{id}
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
