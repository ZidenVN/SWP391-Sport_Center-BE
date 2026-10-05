package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class CoachRequestDTO {
    private Long coachId;
    private String firstName;
    private String lastName;
    private String gender;
    private String phone;
    private String email;
    private String password;
    private String specialty;
}
