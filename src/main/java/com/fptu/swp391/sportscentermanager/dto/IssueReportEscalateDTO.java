package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class IssueReportEscalateDTO {
    // Khớp độ dài cột issue_reports.escalate_reason
    @Size(max = 500, message = "Lý do chuyển tối đa 500 ký tự")
    private String reason;
}
