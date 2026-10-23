package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Người dùng tự sửa hồ sơ: không cho đổi email, vai trò, trạng thái. */
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class MyProfileUpdateDTO {
    @NotBlank(message = "Họ không được để trống")
    @Size(max = 50, message = "Họ tối đa 50 ký tự")
    private String firstName;

    @NotBlank(message = "Tên không được để trống")
    @Size(max = 50, message = "Tên tối đa 50 ký tự")
    private String lastName;

    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(regexp = "(?i)MALE|FEMALE|OTHER", message = "Giới tính không hợp lệ")
    private String gender;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;
}
