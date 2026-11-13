package com.fptu.swp391.sportscentermanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 1 buổi trong lịch sắp tới của hội viên
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class MemberSessionResponseDTO {
    private Long sessionId;
    private Long classId;
    private String classCode;   // mã lớp hiển thị, vd YOG-0012
    private String subjectName;
    private String roomName;
    private String coachName;        // người dạy buổi này
    private boolean substitute;      // true = HLV dạy thay
    private String mainCoachName;    // HLV chính của lớp
    private String type;             // REGULAR | MAKEUP
    private String status;           // SCHEDULED | POSTPONED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String note;
}
