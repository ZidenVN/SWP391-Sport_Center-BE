package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** BE-25: Manager tạo tài khoản lễ tân. */
@Getter @Setter
@NoArgsConstructor
public class StaffCreateRequestDTO extends StaffUpdateRequestDTO {
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 64, message = "Mật khẩu từ 6 đến 64 ký tự")
    private String password;
}
