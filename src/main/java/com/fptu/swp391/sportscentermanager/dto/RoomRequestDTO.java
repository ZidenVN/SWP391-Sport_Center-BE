package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomRequestDTO {
    @NotBlank(message = "Tên phòng không được trống")
    private String roomName;

    @NotNull
    @Min(value = 1, message = "Sức chứa tối thiểu phải là 1")
    private Integer capacity;

    @Pattern(regexp = "AVAILABLE|MAINTENANCE", message = "Trạng thái phòng chỉ nhận AVAILABLE hoặc MAINTENANCE")
    private String status;
}
