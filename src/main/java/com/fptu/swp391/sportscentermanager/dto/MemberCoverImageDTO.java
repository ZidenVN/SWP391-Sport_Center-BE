package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

// Đổi ảnh bìa hồ sơ: chỉ nhận tên 1 ảnh có sẵn ở FE (/landing/<tên>.jpg), không cần gửi các trường hồ sơ khác
@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class MemberCoverImageDTO {
    @NotBlank(message = "Chưa chọn ảnh bìa")
    @Pattern(regexp = "^(hero|gym|gym-floor|fitness|swim|boxing|yoga|tennis|football|basketball)$", message = "Ảnh bìa không hợp lệ")
    private String coverImage;
}
