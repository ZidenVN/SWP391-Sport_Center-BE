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
    PHONE_ALREADY_TAKEN(HttpStatus.BAD_REQUEST, "ERR_USER_002", "Số điện thoại đã được sử dụng!"),
    CLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_CLASS_404", "Lớp học không tồn tại!"),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_MEMBER_404","Lớp học khoonh tồn tại!"),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_SUBSCRIPTION_404","Gói đăng ký không tồn tại!"),
    REGISTRATION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_REGISTRATION_404","Chưa đăng ký lớp này!"),
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND,"ERR_TICKET_404","Yêu cầu hỗ trợ không tồn tại!"),

    NO_ACTIVE_SUBSCRIPTION(HttpStatus.BAD_REQUEST,"ERR_SUBSCRIPTION_001","Hội viên chưa có gói tập co hiệu lực!"),
    SUBSCRIPTION_EXPIRED(HttpStatus.BAD_REQUEST,"ERR_SUBSCRIPTION_002","Gói tập đã hết hạn!"),
    CLASS_FULL(HttpStatus.CONFLICT,"ERR_CLASS_001","Lớp học đã đủ số lượng!"),
    ALREADY_REGISTERED(HttpStatus.CONFLICT,"ERR_CLASS_002","Bạn đã đăng ký lớp học này rồi!"),
    CLASS_NOT_OPEN(HttpStatus.BAD_REQUEST,"ERR_CLASS_003","Lớp học không còn mở đăng ký!"),
    ALREADY_CHECK_IN(HttpStatus.CONFLICT,"ERR_CHECKIN_001","Hội viên đã check-in rồi!")
    // TODO: thêm các code error nếu cần.
        ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
