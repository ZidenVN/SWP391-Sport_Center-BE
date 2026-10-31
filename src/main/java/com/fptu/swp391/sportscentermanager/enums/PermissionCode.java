package com.fptu.swp391.sportscentermanager.enums;

import lombok.Getter;

@Getter

public enum PermissionCode {
    VIEW_PACKAGE("Xem gói tập"),
    MANAGE_PACKAGE("Quản lý gói tập"),
    VIEW_ROOM("Xem phòng tập"),
    MANAGE_ROOM("Quản lý phòng tập"),
    VIEW_SUBJECT("Xem môn học"),
    MANAGE_SUBJECT("Quản lý môn học"),
    VIEW_CLASS("Xem lớp học"),
    MANAGE_CLASS("Quản lý lớp học"),
    REGISTER_CLASS("Đăng ký/hủy lớp học"),
    CHECK_IN_MEMBER("Quẹt thẻ điểm danh ở cửa"),
    MARK_ATTENDANCE("Điểm danh trong lớp"),
    VIEW_ATTENDANCE("Xem lịch sử điểm danh"),
    CREATE_PAYMENT("Tạo thanh toán"),
    VIEW_PAYMENT("Xem thanh toán"),
    APPROVE_REFUND("Duyệt hoàn tiền"),
    VIEW_USER("Xem danh sách người dùng"),
    CREATE_USER("Tạo người dùng mới"),
    UPDATE_USER("Cập nhật người dùng"),
    DEACTIVATE_USER("Khóa người dùng"),
    MANAGE_ROLE("Quản lý phân quyền"),
    VIEW_OWN_PROFILE("Xem hồ sơ cá nhân"),
    UPDATE_OWN_PROFILE("Cập nhật hồ sơ cá nhân"),
    VIEW_OWN_SCHEDULE("Xem lịch cá nhân"),
    // ---- Member / Receptionist ----
    SUBSCRIBE_PACKAGE("Đăng ký/gia hạn gói tập (hội viên tự thực hiện)"), //Member tự đăng ký hoặc gia hạn gói
    MANAGE_SUBSCRIPTION("Quản lý đăng ký gói tập của hội viên"), //Lễ tân bán hoặc gia hạn gói cho member
    SEARCH_MEMBER("Tìm kiếm thông tin hội viên"), //Lễ tân tìm member
    MANAGE_TICKET("Quản lý yêu cầu hỗ trợ"),//Lễ tân ghi nhận và xem yêu cầu hỗ trợ
    MANAGE_QUIZ("Quản lý câu hỏi quiz hội viên"),
    SEND_NOTIFICATION("Gửi thông báo cho hội viên"),
    MANAGE_CLASS_SESSION("Quản lý buổi học: dạy thay, bảo lưu, buổi bù"),
    SWITCH_CLASS_REGISTRATION("Đổi lớp giúp hội viên"),
    SUBMIT_ISSUE_REPORT("Gửi report sự cố"),
    HANDLE_ISSUE_REPORT("Xử lý report sự cố")

    ;
    private final String description;
    PermissionCode(String description) { this.description = description; }
}
