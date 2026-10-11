package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TakeAttendanceRequestDTO {
    @NotEmpty(message = "Danh sách điểm danh không được để trống")
    private List<AttendanceRecordDTO> records;
}
