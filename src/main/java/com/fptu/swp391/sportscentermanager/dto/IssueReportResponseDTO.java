package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.IssueReportCategory;
import com.fptu.swp391.sportscentermanager.enums.IssueReportDesk;
import com.fptu.swp391.sportscentermanager.enums.IssueReportStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class IssueReportResponseDTO {
    private Long reportId;
    private Long reporterId;
    private String reporterName;
    private String reporterEmail;
    private String reporterPhone;
    private String reporterRole;

    private IssueReportCategory category;
    private String categoryLabel;

    private String title;
    private String content;
    private String imageUrl;

    private IssueReportStatus status;
    private IssueReportDesk desk;

    private Long handledById;
    private String handledByName;
    private String response;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
