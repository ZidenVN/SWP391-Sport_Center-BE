package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

@Getter
@Setter
@Builder //toString
@NoArgsConstructor
@AllArgsConstructor
public class MemberProfileResponseDTO {
    private Long memberId;
    private String firstName;
    private String lastName;
    private String gender;
    private String email;
    private String phone;
    private String trainingGoal;
}
