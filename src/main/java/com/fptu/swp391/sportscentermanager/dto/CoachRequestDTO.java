package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class CoachRequestDTO {
    private Long coachId;

    @NotBlank()
    private String firstName;
    @NotBlank()
    private String lastName;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Giới tính phải là MALE, FEMALE hoặc OTHER")
    private String gender;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, max = 64)
    private String password;

    @NotEmpty(message = "HLV phải có ít nhất 1 môn chuyên môn")
    private Set<Long> subjectIds;
}
