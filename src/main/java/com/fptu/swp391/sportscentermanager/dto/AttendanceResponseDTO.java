package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.AttendanceStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceResponseDTO {
    private Long attendanceId;
    private Long memberId;
    private String memberName;
    private String phone;
    private LocalDateTime attendanceDate;
    private AttendanceStatus status;
    private LocalDateTime markedAt;
    private String statusDescription;
    private String note;
}
