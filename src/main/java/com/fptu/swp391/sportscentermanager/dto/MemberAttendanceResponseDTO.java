package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.AttendanceStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class MemberAttendanceResponseDTO {
    private Long attendanceId;
    private Long classId;
    private String classCode;   // mã lớp hiển thị, vd YOG-0012
    private Long sessionId;
    private String subjectName;
    private String coachName;
    private String roomName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AttendanceStatus status;
    private String statusDescription; // mô tả tiếng Việt của status, vd "Có mặt"
    private String note;
    private LocalDateTime markedAt;
}
