package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalkInMemberRequestDTO {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @NotBlank private String gender;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0")
    private String phone;

    // Không bắt buộc: nếu để trống sẽ dùng "Chưa cập nhật"
    private String trainingGoal;
}
