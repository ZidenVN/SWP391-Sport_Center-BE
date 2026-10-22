package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRegistrationResponseDTO {
    private Long registrationId;
    private Long memberId;
    private Long classId;
    private String subjectName;
    private String roomName;
    private String coachName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;           // REGISTERED | CANCELLED
    private LocalDateTime registeredAt;
    private LocalDateTime cancelledAt;
}
