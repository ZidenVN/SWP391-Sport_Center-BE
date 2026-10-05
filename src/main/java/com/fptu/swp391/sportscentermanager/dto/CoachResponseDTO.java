package com.fptu.swp391.sportscentermanager.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
@Builder @RequiredArgsConstructor
public class CoachResponseDTO {
    private Long coachId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String speciality;
}
