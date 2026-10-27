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

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_MEMBER_404", "Hội viên không tồn tại!"),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_SUBSCRIPTION_404", "Gói đăng ký không tồn tại!"),
    REGISTRATION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_REGISTRATION_404", "Chưa đăng ký lớp này!"),
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_TICKET_404", "Yêu cầu hỗ trợ không tồn tại!"),

    NO_ACTIVE_SUBSCRIPTION(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_SUBSCRIPTION_001", "Hội viên chưa có gói tập còn hiệu lực!"),
    SUBSCRIPTION_EXPIRED(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_SUBSCRIPTION_002", "Gói tập không còn hiệu lực vào ngày diễn ra lớp học"),
    CLASS_FULL(HttpStatus.CONFLICT, "ERR_CLASS_001", "Lớp học đã đủ số lượng!"),
    ALREADY_REGISTERED(HttpStatus.CONFLICT, "ERR_CLASS_002", "Bạn đã đăng ký lớp học này rồi!"),
    CLASS_NOT_OPEN(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_CLASS_003", "Lớp học không còn mở đăng ký!"),
    INVALID_CLASS_TIME(HttpStatus.BAD_REQUEST, "ERR_CLASS_004", "Giờ kết thúc phải sau giờ bắt đầu"),
    ROOM_SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "ERR_CLASS_005", "Phòng đã có lớp khác trong khung giờ này!"),
    COACH_SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "ERR_CLASS_006", "Huấn luyện viên đã có lớp khác trong khung giờ này!"),
    MEMBER_SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "ERR_REGISTRATION_003", "Bạn đã đăng ký một lớp học khác trùng giờ này!"),
    ROOM_UNAVAILABLE(HttpStatus.BAD_REQUEST, "ERR_ROOM_001", "Phòng đang bảo trì, không thể xếp lớp!"),
    ROOM_CAPACITY_BELOW_CLASS(HttpStatus.BAD_REQUEST, "ERR_ROOM_002", "Phòng đang có lớp cần nhiều chỗ hơn sức chứa mới!"),
    CLASS_CAPACITY_EXCEEDS_ROOM(HttpStatus.BAD_REQUEST, "ERR_CLASS_007", "Sức chứa lớp vượt quá sức chứa của phòng!"),
    CAPACITY_BELOW_REGISTERED(HttpStatus.BAD_REQUEST, "ERR_CLASS_008", "Sức chứa mới nhỏ hơn số học viên đã đăng ký!"),


    ALREADY_CHECK_IN(HttpStatus.CONFLICT, "ERR_CHECKIN_001", "Hội viên đã check-in rồi!"),
    MEMBER_NOT_IN_CLASS(HttpStatus.BAD_REQUEST, "ERR_ATTENDANCE_001", "Học viên không thuộc danh sách lớp học này!"),
    REGISTRATION_ALREADY_CANCELLED(HttpStatus.CONFLICT, "ERR_REGISTRATION_001", "Đăng ký này đã được hủy trước đó!"),
    CANCEL_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_REGISTRATION_002", "Đã quá hạn huỷ đăng ký lớp học này!"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_PAYMENT_404", "Giao dịch thanh toán không tồn tại!"),

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "ERR_REQUEST_001", "Yêu cầu không hợp lệ!"),
    DUPLICATE_ATTENDANCE(HttpStatus.BAD_REQUEST, "ERR_ATTENDANCE_002", "Học viên bị trùng trong danh sách điểm danh!"),
    RESOUCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_RESOURCE_404", "Tài nguyên yêu cầu không tồn tại"),
    ROLE_NOT_MATCH_USER_TYPE(HttpStatus.BAD_REQUEST, "ERR_ROLE_400", "Role không phù hợp với loại tài khoản!"),
    COACH_INACTIVE(HttpStatus.BAD_REQUEST, "ERR_COACH_400", "Huấn luyện viên đang bị khóa, không thể phân công!"),
    PAYMENT_PENDING_EXISTS(HttpStatus.CONFLICT, "ERR_PAYMENT_409", "Bạn đang có giao dịch chờ thanh toán, vui lòng hoàn tất hoặc thử lại sau 15 phút!"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ERR_AUTH_403", "Bạn không có quyền truy cập dữ liệu này!"),
    CLASS_ALREADY_STARTED(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_CLASS_011", "Lớp học đã bắt đầu, không thể xoá!"),

    // BE-15 → BE-27
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "ERR_REQUEST_002", "Khoảng thời gian không hợp lệ: fromDate phải trước hoặc bằng toDate!"),
    CLASS_ALREADY_CLOSED(HttpStatus.CONFLICT, "ERR_CLASS_009", "Lớp học đã bị hủy hoặc đã kết thúc, không thể thay đổi!"),
    CLASS_NOT_STARTED(HttpStatus.BAD_REQUEST, "ERR_CLASS_010", "Lớp học chưa kết thúc, không thể đánh dấu hoàn thành!"),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "ERR_PAYMENT_001", "Giao dịch đã được xử lý trước đó!"),
    PAYMENT_METHOD_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ERR_PAYMENT_002", "Giao dịch online chỉ được xác nhận qua cổng thanh toán!"),
    TRANSACTION_REF_REQUIRED(HttpStatus.BAD_REQUEST, "ERR_PAYMENT_003", "Chuyển khoản phải nhập mã giao dịch để đối soát!"),
    REFUND_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ERR_PAYMENT_004", "Chỉ hoàn tiền được cho giao dịch đã thanh toán!"),
    INVOICE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "ERR_INVOICE_001", "Chỉ xuất hóa đơn cho giao dịch đã thanh toán!"),
    RECEPTIONIST_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_RECEPTIONIST_404", "Lễ tân không tồn tại!"),
    COACH_HAS_UPCOMING_CLASSES(HttpStatus.CONFLICT, "ERR_COACH_409", "Huấn luyện viên đang có lớp sắp dạy, hãy đổi HLV cho các lớp đó trước khi khóa!"),
    WRONG_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "ERR_AUTH_001", "Mật khẩu hiện tại không đúng!"),
    NEW_PASSWORD_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "ERR_AUTH_002", "Mật khẩu mới phải khác mật khẩu hiện tại!"),
    CANNOT_CHANGE_OWN_STATUS(HttpStatus.BAD_REQUEST, "ERR_USER_003", "Không thể tự khoá hoặc mở khoá tài khoản của chính mình!"),
    ROOM_IN_USE(HttpStatus.CONFLICT, "ERR_ROOM_003", "Phòng đang có lớp học sắp diễn ra, hãy huỷ hoặc đổi phòng cho các lớp đó trước!"),
    SUBJECT_IN_USE(HttpStatus.CONFLICT, "ERR_SUB_409", "Môn học đang có lớp sắp diễn ra, hãy huỷ các lớp đó trước!"),
    MEMBER_INACTIVE(HttpStatus.BAD_REQUEST, "ERR_CHECKIN_002", "Tài khoản hội viên đang bị khoá!"),

    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "ERR_IMG_001", "Vui lòng chọn ảnh cho lớp học"),
    IMAGE_VALID_TYPE(HttpStatus.BAD_REQUEST, "ERR_IMG_002", "Chỉ hỗ trợ ảnh JPG, PNG, WEBP, HEIC!"),
    IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "ERR_IMG_003", "Ảnh tối đa 10MB!"),
    IMAGE_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "ERR_IMG_004", "Upload ảnh thất bại, vui lòng thử lại!"),
    // Gói tập theo buổi
    PACKAGE_NOT_ALLOW_CLASS(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_REGISTRATION_004", "Gói tự do không dùng để đăng ký lớp, vui lòng mua gói lớp!"),
    SUBJECT_NOT_COVERED(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_REGISTRATION_005", "Gói của bạn không áp dụng cho môn của lớp này!"),
    NO_SESSIONS_LEFT(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_REGISTRATION_006", "Gói của bạn đã hết buổi học!"),
    FREE_PACKAGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ERR_PACKAGE_001", "Không được tạo gói tự do cho môn chỉ dành cho lớp (CLASS_ONLY)!"),
    PACKAGE_SUBJECT_REQUIRED(HttpStatus.BAD_REQUEST, "ERR_PACKAGE_002", "Gói lớp phải áp dụng cho ít nhất 1 môn!"),
    INVALID_PACKAGE_CONFIG(HttpStatus.BAD_REQUEST, "ERR_PACKAGE_003", "Gói tự do chỉ áp dụng cho tối đa 1 môn và không có số buổi!"),
    PACKAGE_IN_USE(HttpStatus.CONFLICT, "ERR_PACKAGE_409", "Gói đã có người mua, chỉ được sửa tên, mô tả, giá, trạng thái!"),
    SUBJECT_HAS_ACTIVE_FREE_PACKAGE(HttpStatus.CONFLICT, "ERR_SUB_410", "Môn đang có gói tự do còn bán, hãy ngừng bán các gói đó trước!"),
    SUBJECT_HAS_ACTIVE_PACKAGE(HttpStatus.CONFLICT, "ERR_SUB_411", "Môn đang có gói còn bán, hãy ngừng bán các gói đó trước!"),


    AI_DAILY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "ERR_AI_001", "Bạn đã dùng hết lượt hỏi AI hôm nay, vui lòng quay lại vào ngày mai!"),
    AI_PROVIDER_BUSY(HttpStatus.SERVICE_UNAVAILABLE, "ERR_AI_002", "Trợ lý AI đang quá tải, vui lòng thử lại sau ít phút!"),
    AI_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "ERR_AI_003", "Trợ lý AI tạm thời không phản hồi, vui lòng thử lại!"),
    AI_CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_AI_004", "Cuộc trò chuyện không tồn tại!"),

    QUIZ_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_QUIZ_404", "Câu hỏi không tồn tại!"),
    QUIZ_CODE_TAKEN(HttpStatus.BAD_REQUEST, "ERR_QUIZ_001", "Mã câu hỏi đã được sử dụng!"),
    QUIZ_INVALID_OPTIONS(HttpStatus.BAD_REQUEST, "ERR_QUIZ_002", "Câu chọn 1/chọn nhiều cần ít nhất 2 lựa chọn; câu nhập số/văn bản không có lựa chọn!"),
    QUIZ_INVALID_RANGE(HttpStatus.BAD_REQUEST, "ERR_QUIZ_003", "Giá trị nhỏ nhất phải nhỏ hơn hoặc bằng giá trị lớn nhất!"),
    QUIZ_OPTION_NOT_FOUND(HttpStatus.BAD_REQUEST, "ERR_QUIZ_004", "Lựa chọn không thuộc câu hỏi này!"),
    QUIZ_TYPE_LOCKED(HttpStatus.CONFLICT, "ERR_QUIZ_409", "Câu hỏi đã có hội viên trả lời, không được đổi loại câu!"),
    QUIZ_SYSTEM_QUESTION_LOCKED(HttpStatus.CONFLICT, "ERR_QUIZ_410", "Câu hỏi hệ thống không được sửa hoặc ẩn!"),
    QUIZ_MISSING_ANSWER(HttpStatus.BAD_REQUEST, "ERR_QUIZ_005", "Bạn cần trả lời tất cả câu hỏi!"),
    QUIZ_INVALID_ANSWER(HttpStatus.BAD_REQUEST, "ERR_QUIZ_006", "Câu trả lời không hợp lệ, vui lòng tải lại bộ câu hỏi!"),
    QUIZ_NUMBER_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "ERR_QUIZ_007", "Giá trị nhập nằm ngoài khoảng cho phép!"),
    QUIZ_NOT_COMPLETED(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_QUIZ_008", "Bạn cần hoàn thành bài khảo sát trước khi đăng ký lớp!");



    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
