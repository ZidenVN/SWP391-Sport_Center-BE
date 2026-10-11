package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class SupportTicketResponseDTO {
    private Long ticketId;
    private String content;
    private String status;          // OPEN | RESOLVED
    private LocalDateTime createdDate;
    private Long memberId;
    private String memberName;
    private Long receptionistId;
}
