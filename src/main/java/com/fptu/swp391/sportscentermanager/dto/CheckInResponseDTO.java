package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponseDTO {
    private Long checkInId;
    private Long memberId;
    private String memberName;
    private String phone;
    private LocalDateTime checkInTime;
    private String receptionistName;
    private String packageName;
    private LocalDateTime subscriptionEndDate;
}
