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

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_MEMBER_404","Hội viên không tồn tại!"),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_SUBSCRIPTION_404","Gói đăng ký không tồn tại!"),
    REGISTRATION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_REGISTRATION_404","Chưa đăng ký lớp này!"),
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND,"ERR_TICKET_404","Yêu cầu hỗ trợ không tồn tại!"),

    NO_ACTIVE_SUBSCRIPTION(HttpStatus.BAD_REQUEST,"ERR_SUBSCRIPTION_001","Hội viên chưa có gói tập co hiệu lực!"),
    SUBSCRIPTION_EXPIRED(HttpStatus.BAD_REQUEST,"ERR_SUBSCRIPTION_002","Gói tập đã hết hạn!"),
    CLASS_FULL(HttpStatus.CONFLICT,"ERR_CLASS_001","Lớp học đã đủ số lượng!"),
    ALREADY_REGISTERED(HttpStatus.CONFLICT,"ERR_CLASS_002","Bạn đã đăng ký lớp học này rồi!"),
    CLASS_NOT_OPEN(HttpStatus.BAD_REQUEST,"ERR_CLASS_003","Lớp học không còn mở đăng ký!"),
    INVALID_CLASS_TIME(HttpStatus.BAD_REQUEST, "ERR_CLASS_004", "Giờ kết thúc phải sau giờ bắt đầu"),
    ROOM_SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "ERR_CLASS_005", "Phòng đã có lớp khác trong khung giờ này!"),
    COACH_SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "ERR_CLASS_006", "Huấn luyện viên đã có lớp khác trong khung giờ này"),
    MEMBER_SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "ERR_REGISTRATION_003", "Bạn đã đăng ký một lớp học khác trùng giờ này!"),
    ALREADY_CHECK_IN(HttpStatus.CONFLICT,"ERR_CHECKIN_001","Hội viên đã check-in rồi!"),
    MEMBER_NOT_IN_CLASS(HttpStatus.BAD_REQUEST, "ERR_ATTENDANCE_001", "Học viên không thuộc danh sách lớp học này!"),
    REGISTRATION_ALREADY_CANCELLED(HttpStatus.CONFLICT,"ERR_REGISTRATION_001","Đăng ký này đã được hủy trước đó!"),
    CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST,"ERR_REGISTRATION_002","Lớp học đã bắt đầu, không thể hủy đăng ký!"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_PAYMENT_404", "Giao dịch thanh toán không tồn tại!"),

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "ERR_REQUEST_001", "Yêu cầu không hợp lệ!"),
    DUPLICATE_ATTENDANCE(HttpStatus.BAD_REQUEST, "ERR_ATTENDANCE_002", "Học viên bị trùng trong danh sách điểm danh!"),
    RESOUCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_RESOURCE_404", "Tài nguyên yêu cầu không tồn tại"),
    ROLE_NOT_MATCH_USER_TYPE(HttpStatus.BAD_REQUEST, "ERR_ROLE_400", "Role không phù hợp với loại tài khoản!"),
    COACH_INACTIVE(HttpStatus.BAD_REQUEST, "ERR_COACH_400", "Huấn luyện viên đang bị khóa, không thể phân công!"),
    PAYMENT_PENDING_EXISTS(HttpStatus.CONFLICT, "ERR_PAYMENT_409", "Bạn đang có giao dịch chờ thanh toán, vui lòng hoàn tất hoặc thử lại sau 15 phút!"),

    // BE-15 → BE-27
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "ERR_REQUEST_002", "Khoảng thời gian không hợp lệ: fromDate phải trước hoặc bằng toDate!"),
    CLASS_ALREADY_CLOSED(HttpStatus.CONFLICT, "ERR_CLASS_007", "Lớp học đã bị hủy hoặc đã kết thúc, không thể thay đổi!"),
    CLASS_NOT_STARTED(HttpStatus.BAD_REQUEST, "ERR_CLASS_008", "Lớp học chưa kết thúc, không thể đánh dấu hoàn thành!"),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "ERR_PAYMENT_001", "Giao dịch đã được xử lý trước đó!"),
    PAYMENT_METHOD_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ERR_PAYMENT_002", "Giao dịch online chỉ được xác nhận qua cổng thanh toán!"),
    TRANSACTION_REF_REQUIRED(HttpStatus.BAD_REQUEST, "ERR_PAYMENT_003", "Chuyển khoản phải nhập mã giao dịch để đối soát!"),
    REFUND_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ERR_PAYMENT_004", "Chỉ hoàn tiền được cho giao dịch đã thanh toán!"),
    INVOICE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "ERR_INVOICE_001", "Chỉ xuất hóa đơn cho giao dịch đã thanh toán!"),
    RECEPTIONIST_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_RECEPTIONIST_404", "Lễ tân không tồn tại!"),
    COACH_HAS_UPCOMING_CLASSES(HttpStatus.CONFLICT, "ERR_COACH_409", "Huấn luyện viên đang có lớp sắp dạy, hãy đổi HLV cho các lớp đó trước khi khóa!"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ERR_AUTH_403", "Bạn không có quyền truy cập dữ liệu này!")

















    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
