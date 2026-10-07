package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter @Builder
public class MemberProfileUpdateDTO {
    //Update không cho sửa email
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @NotBlank private String gender;
    @NotBlank private String phone;
    @NotBlank private String trainingGoal;

}
