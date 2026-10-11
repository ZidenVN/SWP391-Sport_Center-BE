package com.fptu.swp391.sportscentermanager.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
public class ClassRosterResponseDTO {
    private Long memberId;
    private String firstName;
    private String lastName;
    private String gender;
    private String email;
    private String phone;
    private String trainingGoal;
    private LocalDateTime enrolledDate;
    private String enrollmentStatus;
}
