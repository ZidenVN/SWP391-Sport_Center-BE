package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDTO {
    @NotBlank(message = "Ho khong duoc de trong")
    private String firstName;

    @NotBlank(message = "Ten khong duoc de trong")
    private String lastName;

    @NotBlank(message = "Goi tinh khong duoc de trong")
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gioi tinh khong hop le")
    private String gender;

    @NotBlank(message = "Email khong duoc de trong")
    @Email(regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$",
    message = "Email không đúng định dạng (Ví dụ đúng: ten@gmail.com)")
    private String email;

    @NotBlank(message = "So dien thoai khong duoc de trong")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "So dien thoai khong hop le")
    private String phone;

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 6, message = "Mat khau toi thieu phai 6 ki tu")
    private String password;
}
