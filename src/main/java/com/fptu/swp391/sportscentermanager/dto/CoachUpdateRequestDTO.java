package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
/** BE-27: Manager sửa thông tin HLV. Không nhận password để tránh reset mật khẩu tùy tiện. */
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class CoachUpdateRequestDTO {
    @NotBlank()
    private String firstName;
    @NotBlank()
    private String lastName;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Giới tính phải là MALE, FEMALE hoặc OTHER")
    private String gender;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotEmpty(message = "HLV phải có ít nhất 1 môn chuyên môn")
    private Set<Long> subjectIds;
}
