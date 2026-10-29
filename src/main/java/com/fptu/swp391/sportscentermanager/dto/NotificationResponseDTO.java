package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Long notificationId;
    private String title;
    private String content;
    private String targetType;
    private LocalDateTime sendDate;
    private String targetName;
    private boolean read;
    private String senderName; // null == system
    private String category;   // SYSTEM / STAFF
}
