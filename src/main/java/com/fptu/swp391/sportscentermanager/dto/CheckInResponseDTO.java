package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class CheckInResponseDTO {
    private Long checkInId;
    private Long memberId;
    private String memberName;
    private String phone;
    private LocalDateTime checkInTime;
    private String checkedInBy;
    private String note;
    // Gói đang dùng lúc check-in để lễ tân nhắc gia hạn
    private String packageName;
    private LocalDateTime packageEndDate;
    private Long daysLeft;
    // Lớp hội viên đã đăng ký trong hôm nay
    private List<TodayClass> todayClasses;

    @Getter @Setter
    @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TodayClass {
        private Long classId;
        private String subjectName;
        private String roomName;
        private String coachName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
