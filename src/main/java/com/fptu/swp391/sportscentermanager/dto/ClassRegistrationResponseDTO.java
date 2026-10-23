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
    private Long coachId;
    private String coachSpeciality;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;           // REGISTERED | CANCELLED
    private LocalDateTime registeredAt;
    private LocalDateTime cancelledAt;
    // Trạng thái lớp (OPENING | CANCELLED | COMPLETED) để hội viên biết lớp bị hủy hay đã diễn ra
    private String classStatus;
    // Kết quả HLV điểm danh buổi này (PRESENT | ABSENT | LATE | ON_LEAVE), null nếu chưa điểm danh
    private String attendanceStatus;
    private String attendanceNote;
}
