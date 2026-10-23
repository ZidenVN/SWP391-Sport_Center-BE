package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/** BE-25: Manager sửa thông tin nhân viên lễ tân. Không đổi mật khẩu ở đây. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class StaffUpdateRequestDTO {
    @NotBlank(message = "Họ không được để trống")
    private String firstName;

    @NotBlank(message = "Tên không được để trống")
    private String lastName;

    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Giới tính phải là MALE, FEMALE hoặc OTHER")
    private String gender;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;
}
