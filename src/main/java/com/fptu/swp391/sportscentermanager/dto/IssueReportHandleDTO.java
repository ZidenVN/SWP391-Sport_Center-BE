package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.IssueReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class IssueReportHandleDTO {
    @NotNull(message = "Trạng thái xử lý không được để trống")
    private IssueReportStatus status;

    private String response;
}
