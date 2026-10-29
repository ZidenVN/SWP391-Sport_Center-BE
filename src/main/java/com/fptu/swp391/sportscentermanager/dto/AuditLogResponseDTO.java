package com.fptu.swp391.sportscentermanager.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AuditLogResponseDTO {
    private Long logId;
    private Long userId;
    private String email;
    private String actionType;
    private String targetEntity;
    private Long targetEntityId;
    private String details;
    private String description; // "19:00 ngày 29/10/2026 · Lễ tân Nguyễn A đã ..."
    private LocalDateTime createdAt;
}
