package com.fptu.swp391.sportscentermanager.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_ROOM_404", "Phòng tập không tồn tại!"),
    SUBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_SUB_404", "Môn học không tồn tại!"),
    COACH_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_COACH_404", "Huấn luyện viên không tồn tại!"),
    PACKAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_PACKAGE_404", "Gói tập không tồn tại!"),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_ROLE_404", "Role không tồn tại!"),
    PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_PERMISSION_404", "Permission không tồn tại"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_USER_404", "Người dùng không tồn tại!"),
    EMAIL_ALREADY_TAKEN(HttpStatus.BAD_REQUEST, "ERR_USER_001", "Email đã được sử dụng!"),
    PHONE_ALREADY_TAKEN(HttpStatus.BAD_REQUEST, "ERR_USER_002", "Số điện thoại đã được sử dụng!")

    // TODO: thêm các code error nếu cần.
        ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
