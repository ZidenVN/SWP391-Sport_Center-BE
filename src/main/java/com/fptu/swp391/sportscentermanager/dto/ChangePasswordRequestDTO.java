package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequestDTO {
    @NotBlank(message = "Mật khẩu hiện tại không được để trống!")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống!")
    @Size(min = 6, max = 64, message = "Mật khẩu mới phải từ 6 đến 64 ký tự")
    private String newPassword;
}
