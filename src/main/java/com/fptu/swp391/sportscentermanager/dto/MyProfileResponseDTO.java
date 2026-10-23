package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.util.List;

/** Hồ sơ của chính người đang đăng nhập (mọi vai trò): GET/PUT /api/v1/users/me */
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class MyProfileResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String gender;
    private String email;
    private String phone;
    private String status;
    private String roleName;
    // Chỉ HLV có; Quản lý cập nhật, HLV chỉ xem
    private String speciality;
    // Quyền hiện tại của vai trò (đọc từ DB) để FE cập nhật ngay khi Quản lý đổi phân quyền
    private List<String> permissions;
}
