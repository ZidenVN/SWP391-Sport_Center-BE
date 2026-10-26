package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter @Builder
public class MemberProfileUpdateDTO {
    //Update không cho sửa email
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Giới tính không hợp lệ")
    private String gender;
    @NotBlank private String phone;
    @NotBlank private String trainingGoal;

}
