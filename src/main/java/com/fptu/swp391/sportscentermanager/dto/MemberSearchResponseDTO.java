package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
public class MemberSearchResponseDTO {
    private Long memberId;
    private String firstName;
    private String lastName;
    private String gender;
    private String email;
    private String phone;
    private String status;
    private String trainingGoal;
}
