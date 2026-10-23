package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class CheckInRequestDTO {
    @NotNull(message = "Chưa chọn hội viên")
    private Long memberId;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    private String note;
}
