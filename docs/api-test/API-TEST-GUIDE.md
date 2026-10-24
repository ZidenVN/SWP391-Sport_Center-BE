# Sports Center – Tài liệu test API

Tài liệu dùng để test toàn bộ backend. Gồm 2 phần: **hướng dẫn chạy bộ test Postman** và **bảng tra cứu tất cả API**.

## 1. Chạy bộ test tự động

File: `SportsCenter-Full-API.postman_collection.json` (cùng thư mục với tài liệu này).

**Chuẩn bị**

1. Chạy backend trên máy, **trỏ vào DB local (Docker)**. Bộ test tạo và xoá mềm dữ liệu, không nên chạy trên DB Railway chung của nhóm.
2. Postman → **Import** file collection.
3. Bấm tên collection → tab **Variables**, điền cột **Current value**:

| Biến | Giá trị |
|---|---|
| `base_url` | `http://localhost:8080` (mặc định) |
| `managerEmail` / `managerPassword` | Tài khoản Manager (mặc định `manager@sportcenter.com`) |
| `receptionistEmail` / `receptionistPassword` | Tài khoản Lễ tân mặc định (`receptionist@Sportcenter.com`) |
| `testPassword` | Mật khẩu cho các tài khoản test được tạo trong lúc chạy (mặc định `QaTest@12345`) |

4. Bấm **Run** → giữ nguyên thứ tự → **Run Sports Center - Full API Test**.

**Bộ test tự làm gì**

Mỗi lần chạy sinh một hậu tố `{{run}}` (8 chữ số) để tạo dữ liệu riêng, không đụng vào dữ liệu thật:

| Thư mục | Nội dung |
|---|---|
| 00. Setup | Đăng nhập Manager + Lễ tân, sinh hậu tố và các mốc giờ (lớp học bắt đầu sau 2 ngày, 09:00) |
| 01. Danh mục | Tạo/sửa môn, phòng (20 chỗ), gói tập |
| 02. Nhân sự & tài khoản | Tạo Coach, Lễ tân; tìm kiếm user có phân trang; chặn trùng SĐT, tự khoá, gán role sai loại |
| 03. Lễ tân | Tạo hội viên tại quầy → đăng ký gói (PENDING) → xác nhận thu tiền (COMPLETED) → hoá đơn → hội viên đăng nhập bằng mật khẩu tạm |
| 04. Lớp học | Tạo lớp; chặn trùng phòng, giờ sai, vượt sức chứa; tìm lớp; sửa; gán coach; huỷ lớp |
| 05. Đặt lớp | Đặt lớp, chặn đặt trùng, tự huỷ, Lễ tân đặt hộ |
| 06. Coach | Lịch dạy, roster, điểm danh, giáo án, kết quả tập, tiến độ, thông báo |
| 07. Hội viên | Hồ sơ, gói, thanh toán, hoá đơn, giáo án, kết quả, thông báo |
| 08. Hỗ trợ & thanh toán khác | Ticket, huỷ thanh toán chờ, link VNPay, báo cáo, nhật ký, xuất CSV |
| 09. Phân quyền | 401 khi thiếu/sai token; 403 khi sai role (gồm 3 lỗ hổng cũ của Lễ tân) |
| 10. Tài khoản | Đăng ký, login sai, đổi mật khẩu (token cũ bị vô hiệu), đăng xuất |
| 11. Dọn dữ liệu | Huỷ đăng ký (kể cả huỷ lần 2), hoàn tiền, xoá lớp, khoá coach/lễ tân/hội viên test, xoá mềm phòng/môn/gói |

**Kết quả mong đợi:** tất cả test **Passed**. Request nào Failed → mở request đó xem response và so với cột *Mong đợi* trong tên request.

**API không nằm trong bộ test tự động (test tay):**

| API | Lý do |
|---|---|
| `POST /api/v1/auth/google` | Cần ID token thật từ Google (đăng nhập trên frontend) |
| `GET /api/v1/payments/vnpay/callback` | Cần chữ ký thật do VNPay gửi về sau khi thanh toán sandbox |
| `PUT /api/v1/roles/{id}/permissions` | **Ghi đè** toàn bộ quyền của role, chạy tự động dễ làm hỏng phân quyền |
| `PATCH /api/v1/coaches/member-plans/{memberPlanId}/status` | API không trả `memberPlanId` ở đâu để bộ test lấy tự động |

## 2. Quy ước chung

- Base URL: `http://localhost:8080`. Mọi API (trừ *Công khai*) cần header `Authorization: Bearer <token>` lấy từ `POST /api/v1/auth/login`.
- Thời gian dạng `yyyy-MM-ddTHH:mm:ss`, **giờ Việt Nam**, không có múi giờ. Ngày lọc dạng `yyyy-MM-dd`.
- Lỗi nghiệp vụ trả JSON: `{ "status": 409, "error": "ERR_CLASS_001", "message": "..." }`. Lỗi validate trả `{ "tênTrường": "thông báo" }` với HTTP 400.

| HTTP | Ý nghĩa |
|---|---|
| 400 | Dữ liệu gửi lên sai định dạng / vi phạm ràng buộc đơn giản |
| 401 | Chưa đăng nhập, token sai/hết hạn, đã đăng xuất hoặc đổi mật khẩu (`ERR_AUTH_401`) |
| 403 | Đã đăng nhập nhưng không có quyền (`ERR_AUTH_403`) |
| 404 | Không tìm thấy (hoặc cố ý ẩn dữ liệu của người khác) |
| 409 | Xung đột với dữ liệu hiện có: lớp đầy, đã đăng ký, trùng giờ, giao dịch chờ |
| 422 | Không đủ điều kiện nghiệp vụ: chưa có gói, gói không phủ ngày học, lớp không mở, quá hạn huỷ |

## 3. Bảng tra cứu API

Tổng cộng **107 API**.

### Xác thực

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Công khai (không cần token) | `AuthRequest` |  |
| `POST` | `/api/v1/auth/register` | Công khai (không cần token) | `RegisterRequestDTO` |  |
| `POST` | `/api/v1/auth/google` | Công khai (không cần token) | `GoogleLoginRequest` |  |
| `PUT` | `/api/v1/auth/change-password` | Đã đăng nhập | `ChangePasswordRequestDTO` |  |
| `POST` | `/api/v1/auth/logout` | Đã đăng nhập |  |  |

### Tài khoản (Manager)

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/users` | Manager |  | `keyword`, `role`, `status`, `page`, `size` |
| `GET` | `/api/v1/users/{id}` | Manager |  |  |
| `PUT` | `/api/v1/users/{id}` | Manager | `UserUpdateRequestDTO` |  |
| `PATCH` | `/api/v1/users/{id}/status` | Manager |  |  |
| `PATCH` | `/api/v1/users/{userId}/role/{roleId}` | Manager |  |  |

### Role

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/roles` | Mọi user đã đăng nhập |  |  |
| `GET` | `/api/v1/roles/{id}/permissions` | quyền `MANAGE_ROLE` |  |  |
| `PUT` | `/api/v1/roles/{id}/permissions` | quyền `MANAGE_ROLE` | `List<Long>` |  |

### Permission

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/permissions` | quyền `MANAGE_ROLE` |  |  |

### Nhân viên Lễ tân (Manager)

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/manager/receptionists` | Manager + quyền `VIEW_USER` |  | `keyword`, `status` |
| `GET` | `/api/v1/manager/receptionists/{id}` | Manager + quyền `VIEW_USER` |  |  |
| `POST` | `/api/v1/manager/receptionists` | Manager + quyền `CREATE_USER` | `StaffCreateRequestDTO` |  |
| `PUT` | `/api/v1/manager/receptionists/{id}` | Manager + quyền `UPDATE_USER` | `StaffUpdateRequestDTO` |  |
| `PATCH` | `/api/v1/manager/receptionists/{id}/status` | Manager + quyền `DEACTIVATE_USER` |  |  |

### Coach

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/coaches/{id}` | quyền `VIEW_USER` |  |  |
| `POST` | `/api/v1/coaches` | Manager | `CoachRequestDTO` |  |
| `GET` | `/api/v1/coaches` | quyền `VIEW_USER` |  |  |
| `PUT` | `/api/v1/coaches/{id}` | Manager | `CoachUpdateRequestDTO` |  |
| `DELETE` | `/api/v1/coaches/{id}` | Manager |  |  |
| `GET` | `/api/v1/coaches/me/schedules` | Coach + quyền `VIEW_OWN_SCHEDULE` |  |  |
| `GET` | `/api/v1/coaches/me/classes/{classId}/roster` | Coach + quyền `VIEW_CLASS` |  |  |
| `GET` | `/api/v1/coaches/members/{memberId}` | Coach |  |  |
| `POST` | `/api/v1/coaches/me/classes/{classId}/attendances` | Coach + quyền `MARK_ATTENDANCE` | `TakeAttendanceRequestDTO` |  |
| `GET` | `/api/v1/coaches/me/classes/{classId}/attendances` | Coach + quyền `VIEW_ATTENDANCE` |  |  |
| `POST` | `/api/v1/coaches/me/workout-plans` | Coach | `WorkoutPlanRequestDTO` |  |
| `GET` | `/api/v1/coaches/me/workout-plans` | Coach |  |  |
| `GET` | `/api/v1/coaches/members/{memberId}/workout-plans` | Coach |  |  |
| `GET` | `/api/v1/coaches/classes/{classId}/workout-plans` | Coach / Member |  |  |
| `POST` | `/api/v1/coaches/members/{memberId}/workout-results` | Coach | `WorkoutResultRequestDTO` |  |
| `GET` | `/api/v1/coaches/members/{memberId}/workout-results` | Coach |  |  |
| `PUT` | `/api/v1/coaches/workout-results/{resultId}` | Coach | `WorkoutResultRequestDTO` |  |
| `PATCH` | `/api/v1/coaches/member-plans/{memberPlanId}/status` | Coach |  | `status` |
| `GET` | `/api/v1/coaches/members/{memberId}/progress` | Coach |  |  |
| `POST` | `/api/v1/coaches/classes/{classId}/notifications` | Coach | `NotificationRequestDTO` |  |
| `POST` | `/api/v1/coaches/members/{memberId}/notifications` | Coach | `NotificationRequestDTO` |  |
| `GET` | `/api/v1/coaches/classes/{classId}/notifications` | Coach |  |  |

### Hội viên

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/members/me` | Member + quyền `VIEW_OWN_PROFILE` |  |  |
| `PUT` | `/api/v1/members/me` | Member + quyền `UPDATE_OWN_PROFILE` | `MemberProfileUpdateDTO` |  |
| `GET` | `/api/v1/members/me/workout-plans` | Member |  |  |
| `GET` | `/api/v1/members/{memberId}` | Coach / Manager / Lễ tân |  |  |
| `GET` | `/api/v1/members/me/workout-results` | Member |  |  |
| `GET` | `/api/v1/members/me/progress` | Member |  |  |
| `GET` | `/api/v1/members/me/notifications` | Member |  |  |

### Gói của hội viên

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/members/me/subscriptions` | Member + quyền `VIEW_OWN_PROFILE` |  |  |
| `GET` | `/api/v1/members/me/subscriptions/current` | Member + quyền `VIEW_OWN_PROFILE` |  |  |

### Đặt lớp (hội viên)

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `POST` | `/api/v1/members/me/registrations` | Member + quyền `REGISTER_CLASS` | `ClassRegistrationRequestDTO` |  |
| `DELETE` | `/api/v1/members/me/registrations/{registrationId}` | Member + quyền `REGISTER_CLASS` |  |  |
| `GET` | `/api/v1/members/me/registrations` | Member + quyền `VIEW_OWN_SCHEDULE` |  |  |
| `GET` | `/api/v1/members/me/schedule` | Member + quyền `VIEW_OWN_SCHEDULE` |  |  |

### Thanh toán (hội viên)

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/members/me/payments` | Member + quyền `VIEW_OWN_PROFILE` |  |  |
| `GET` | `/api/v1/members/me/payments/{paymentId}/invoice` | Member + quyền `VIEW_OWN_PROFILE` |  |  |

### VNPay

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `POST` | `/api/v1/payments/vnpay/create-url` | Member + quyền `SUBSCRIBE_PACKAGE` |  | `packageId` |
| `GET` | `/api/v1/payments/vnpay/callback` | Công khai (không cần token) |  |  |

### Lễ tân: hội viên & gói

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/receptionist/members` | Lễ tân + quyền `SEARCH_MEMBER` |  | `keyword` |
| `GET` | `/api/v1/receptionist/members/{memberId}` | Lễ tân + quyền `SEARCH_MEMBER` |  |  |
| `POST` | `/api/v1/receptionist/members` | Lễ tân + quyền `CREATE_USER` | `WalkInMemberRequestDTO` |  |
| `POST` | `/api/v1/receptionist/members/{memberId}/subscriptions` | Lễ tân + quyền `MANAGE_SUBSCRIPTION` | `SubscriptionRequestDTO` |  |
| `GET` | `/api/v1/receptionist/members/{memberId}/subscriptions` | Lễ tân + quyền `MANAGE_SUBSCRIPTION` |  |  |
| `GET` | `/api/v1/receptionist/members/{memberId}/subscriptions/current` | Lễ tân + quyền `MANAGE_SUBSCRIPTION` |  |  |

### Lễ tân: đặt lớp hộ

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `POST` | `/api/v1/receptionist/members/{memberId}/registrations` | Lễ tân + quyền `REGISTER_CLASS` | `ClassRegistrationRequestDTO` |  |
| `DELETE` | `/api/v1/receptionist/members/{memberId}/registrations/{registrationId}` | Lễ tân + quyền `REGISTER_CLASS` |  |  |
| `GET` | `/api/v1/receptionist/members/{memberId}/registrations` | Lễ tân + quyền `REGISTER_CLASS` |  |  |

### Lễ tân: thanh toán

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/receptionist/members/{memberId}/payments` | Lễ tân + quyền `VIEW_PAYMENT` |  |  |
| `GET` | `/api/v1/receptionist/payments` | Lễ tân + quyền `VIEW_PAYMENT` |  | `fromDate`, `toDate`, `status`, `method`, `keyword`, `page`, `size` |
| `PATCH` | `/api/v1/receptionist/payments/{paymentId}/confirm` | Lễ tân + quyền `CREATE_PAYMENT` |  |  |
| `PATCH` | `/api/v1/receptionist/payments/{paymentId}/cancel` | Lễ tân + quyền `CREATE_PAYMENT` | `PaymentActionRequestDTO` |  |
| `GET` | `/api/v1/receptionist/payments/{paymentId}/invoice` | Lễ tân + quyền `VIEW_PAYMENT` |  |  |

### Lễ tân: hỗ trợ

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `POST` | `/api/v1/receptionist/tickets` | Lễ tân + quyền `MANAGE_TICKET` | `SupportTicketRequestDTO` |  |
| `GET` | `/api/v1/receptionist/tickets` | Lễ tân + quyền `MANAGE_TICKET` |  | `memberId` |
| `PATCH` | `/api/v1/receptionist/tickets/{ticketId}/resolve` | Lễ tân + quyền `MANAGE_TICKET` |  |  |

### Gói tập

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/packages` | Mọi user đã đăng nhập |  |  |
| `GET` | `/api/v1/packages/{id}` | Mọi user đã đăng nhập |  |  |
| `POST` | `/api/v1/packages` | quyền `MANAGE_PACKAGE` | `MembershipPackage` |  |
| `PUT` | `/api/v1/packages/{id}` | quyền `MANAGE_PACKAGE` | `MembershipPackage` |  |
| `DELETE` | `/api/v1/packages/{id}` | quyền `MANAGE_PACKAGE` |  |  |

### Phòng

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/rooms` | Mọi user đã đăng nhập |  |  |
| `GET` | `/api/v1/rooms/{id}` | Mọi user đã đăng nhập |  |  |
| `POST` | `/api/v1/rooms` | quyền `MANAGE_ROOM` | `RoomRequestDTO` |  |
| `PUT` | `/api/v1/rooms/{id}` | quyền `MANAGE_ROOM` | `RoomRequestDTO` |  |
| `DELETE` | `/api/v1/rooms/{id}` | quyền `MANAGE_ROOM` |  |  |

### Môn học

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/subjects` | Mọi user đã đăng nhập |  |  |
| `GET` | `/api/v1/subjects/{id}` | Mọi user đã đăng nhập |  |  |
| `POST` | `/api/v1/subjects` | quyền `MANAGE_SUBJECT` | `SubjectRequestDTO` |  |
| `PUT` | `/api/v1/subjects/{id}` | quyền `MANAGE_SUBJECT` | `SubjectRequestDTO` |  |
| `DELETE` | `/api/v1/subjects/{id}` | quyền `MANAGE_SUBJECT` |  |  |

### Lớp học

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `POST` | `/api/v1/classes` | quyền `MANAGE_CLASS` | `SportClassRequestDTO` |  |
| `GET` | `/api/v1/classes` | quyền `VIEW_CLASS` |  |  |
| `GET` | `/api/v1/classes/search` | quyền `VIEW_CLASS` |  | `fromDate`, `toDate`, `subjectId`, `coachId`, `roomId`, `status`, `available`, `page`, `size` |
| `GET` | `/api/v1/classes/{id}` | quyền `VIEW_CLASS` |  |  |
| `PUT` | `/api/v1/classes/{classId}/coach/{coachId}` | quyền `MANAGE_CLASS` |  |  |
| `PUT` | `/api/v1/classes/{id}` | quyền `MANAGE_CLASS` | `SportClassRequestDTO` |  |
| `PATCH` | `/api/v1/classes/{id}/cancel` | quyền `MANAGE_CLASS` |  |  |
| `PATCH` | `/api/v1/classes/{id}/complete` | quyền `MANAGE_CLASS` |  |  |
| `DELETE` | `/api/v1/classes/{id}` | quyền `MANAGE_CLASS` |  |  |

### Thanh toán toàn trung tâm (Manager)

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/manager/payments` | Manager + quyền `VIEW_PAYMENT` |  | `fromDate`, `toDate`, `status`, `method`, `keyword`, `page`, `size` |
| `GET` | `/api/v1/manager/payments/export` | Manager + quyền `VIEW_PAYMENT` |  | `fromDate`, `toDate`, `status`, `method`, `keyword` |
| `PATCH` | `/api/v1/manager/payments/{paymentId}/refund` | Manager + quyền `APPROVE_REFUND` | `PaymentActionRequestDTO` |  |

### Báo cáo

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/manager/reports/overview` | Manager / Admin |  |  |
| `GET` | `/api/v1/manager/reports/members` | Manager / Admin |  | `fromDate`, `toDate` |
| `GET` | `/api/v1/manager/reports/classes` | Manager / Admin |  | `fromDate`, `toDate` |
| `GET` | `/api/v1/manager/reports/revenue` | Manager / Admin |  | `fromDate`, `toDate` |

### Nhật ký thao tác

| Method | Endpoint | Ai được gọi | Body | Query params |
|---|---|---|---|---|
| `GET` | `/api/v1/admin/audit-logs` | Manager |  | `fromDate`, `toDate`, `actionType`, `targetEntity`, `userId`, `page`, `size` |
