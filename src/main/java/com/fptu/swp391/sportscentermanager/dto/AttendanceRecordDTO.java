package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceRecordDTO {
    @NotNull(message = "ID học viên không được để trống")
    private Long memberId;

    @NotNull(message = "Trạng thái điểm danh không được để trống")
    private AttendanceStatus status;
    private String note;
}
