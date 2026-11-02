package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.IssueReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class IssueReportHandleDTO {
    @NotNull(message = "Trạng thái xử lý không được để trống")
    private IssueReportStatus status;

    // Khớp độ dài cột issue_reports.response
    @Size(max = 1000, message = "Phản hồi tối đa 1000 ký tự")
    private String response;
}
