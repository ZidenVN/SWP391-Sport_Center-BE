package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.SessionStatus;
import com.fptu.swp391.sportscentermanager.enums.SessionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class ClassSessionResponseDTO {
    private Long sessionId;
    private Long classId;
    private String className;
    private Long subjectId;
    private String subjectName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long coachId;
    private String coachName;
    private Long roomId;
    private String roomName;
    private SessionType type;
    private SessionStatus status;
    private Long makeupForSessionId;
    private String note;
}
