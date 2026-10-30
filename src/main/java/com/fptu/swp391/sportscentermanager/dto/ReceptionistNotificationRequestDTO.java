package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionistNotificationRequestDTO {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 100, message = "Tiêu đề tối đa 100 ký tự")
    private String title;

    @NotBlank(message = "Nội dung thông báo không được để trống")
    @Size(max = 1000, message = "Nội dung tối đa 1000 ký tự")
    private String content;

    // MEMBERS: gửi cho các member trong memberIds; ALL_ACTIVE: gửi cho mọi member đang ACTIVE (bỏ qua memberIds)
    @NotBlank(message = "Vui lòng chọn kiểu người nhận (MEMBERS hoặc ALL_ACTIVE)")
    private String targetMode;

    private List<Long> memberIds;
}
