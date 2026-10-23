package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class StaffResponseDTO {
    private Long userId;
    private String firstName;
    private String lastName;
    private String gender;
    private String email;
    private String phone;
    private String status;
    private String roleName;
    private LocalDateTime createdAt;
}
