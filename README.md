# Sports Center Manager – Backend

Backend REST API cho hệ thống quản lý trung tâm thể thao (môn học SWP391). Hệ thống phục vụ 4 vai trò: **Quản lý trung tâm**, **Huấn luyện viên**, **Lễ tân** và **Hội viên**.

## Công nghệ

| Thành phần | Phiên bản / thư viện |
|---|---|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, AOP) |
| Cơ sở dữ liệu | MySQL 8 |
| Xác thực | JWT (jjwt 0.13) và đăng nhập Google |
| Thanh toán | VNPay (sandbox) |
| Tài liệu API | springdoc-openapi (Swagger UI) |
| Khác | Lombok, Maven Wrapper |

## Chức năng chính

| Luồng | Nội dung |
|---|---|
| **1. Người dùng và gói thành viên** | Đăng ký, đăng nhập (email hoặc Google), đổi mật khẩu, đăng xuất. Quản lý HLV, lễ tân, hội viên. Phân quyền theo vai trò. Quản lý gói tập. Mua và gia hạn gói (gói mới nối tiếp gói đang còn hạn) |
| **2. Lớp học và lịch** | Quản lý môn học, phòng tập, lớp học. Phân công HLV. Kiểm tra trùng lịch phòng, HLV, hội viên và sức chứa phòng. Hội viên hoặc lễ tân đăng ký và hủy lớp |
| **3. Thanh toán và báo cáo** | Thanh toán VNPay hoặc thu tiền tại quầy. Hóa đơn, hoàn tiền. Báo cáo tổng quan, hội viên, tình trạng lớp và doanh thu |
| **4. Tập luyện và điểm danh** | Danh sách học viên của lớp, điểm danh, giáo án cá nhân và giáo án lớp, kết quả tập, tiến độ, gửi thông báo |
| **Hệ thống** | Nhật ký thao tác (audit log). Yêu cầu hỗ trợ tại quầy. Job tự động nhắc gói sắp hết hạn và lịch học sắp tới |

## Cấu trúc thư mục

```
src/main/java/com/fptu/swp391/sportscentermanager/
├── annotation/    # @LogAudit
├── aspect/        # AOP ghi audit log
├── config/        # Security, CORS, VNPay, OpenAPI, DataInitializer (seed dữ liệu)
├── controller/    # REST API (/api/v1/...)
├── dto/           # Request / Response
├── entity/        # Entity JPA
├── enums/         # Trạng thái, ErrorCode, PermissionCode
├── exception/     # AppException, GlobalExceptionHandler
├── repository/    # Spring Data JPA
├── scheduler/     # Job chạy định kỳ (nhắc gói, nhắc lịch học)
├── security/      # JWT filter, UserDetails
└── service/       # Business logic (interface và impl/)
```

## Cài đặt và chạy

### 1. Yêu cầu
- JDK 21
- Docker (để chạy MySQL), hoặc một MySQL 8 có sẵn
- Không cần cài Maven, vì dự án đã có sẵn `./mvnw`

### 2. Chạy MySQL bằng Docker

```bash
docker run -d --name sports_center_mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=<mat-khau> -e MYSQL_DATABASE=sportcenter_db mysql:8.0
```

Bảng sẽ được Hibernate tự tạo khi chạy app lần đầu (`ddl-auto=update`).

### 3. Tạo file cấu hình local

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Điền các giá trị trong file `application-local.properties`. File này đã nằm trong `.gitignore`, **không được commit**. Giá trị thật xin từ nhóm.

| Biến | Bắt buộc | Mô tả |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | ✅ | Kết nối MySQL. Nếu chạy Docker local thì dùng `jdbc:mysql://localhost:3306/sportcenter_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh` |
| `JWT_SECRET` | ✅ | Tạo bằng lệnh `openssl rand -base64 32` |
| `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET` | ✅ | Thông tin VNPay sandbox |
| `GOOGLE_CLIENT_ID` | ✅ | OAuth Client ID cho đăng nhập Google |
| `APP_INIT_MANAGER_PASSWORD` | | Mật khẩu tài khoản Manager mặc định. Để trống thì không tạo |
| `APP_INIT_COACH_PASSWORD`, `APP_INIT_RECEPTIONIST_PASSWORD` | | Mật khẩu tài khoản HLV và lễ tân mặc định. Để trống thì không tạo |
| `CORS_ALLOWED_ORIGINS` | | Mặc định `http://localhost:*,http://127.0.0.1:*` |
| `VNPAY_RETURN_URL` | | Trang frontend nhận kết quả thanh toán. Mặc định `http://localhost:5173/payment-result` |
| `BOOKING_CANCEL_CUTOFF_HOURS` | | Hạn chót hủy đăng ký lớp, tính bằng số giờ trước khi lớp bắt đầu. Mặc định `2` |
| `PORT` | | Cổng chạy server. Mặc định `8080` |

### 4. Chạy ứng dụng

```bash
./mvnw spring-boot:run
```

Hoặc trong IntelliJ, chạy class `SportsCenterManagerApplication`.

Server chạy ở `http://localhost:8080`. Khi khởi động, `DataInitializer` sẽ tự seed toàn bộ quyền (Permission), vai trò (Role) và các tài khoản mặc định.

### Tài khoản mặc định

Các tài khoản này chỉ được tạo khi biến mật khẩu tương ứng có giá trị và email đó chưa tồn tại trong DB.

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Quản lý trung tâm | `manager@sportcenter.com` | `APP_INIT_MANAGER_PASSWORD` |
| Huấn luyện viên | `coach@Sportcenter.com` | `APP_INIT_COACH_PASSWORD` |
| Lễ tân | `receptionist@Sportcenter.com` | `APP_INIT_RECEPTIONIST_PASSWORD` |

Hội viên tự đăng ký qua `POST /api/v1/auth/register`, hoặc được lễ tân tạo tại quầy.

## Tài liệu API

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **Postman:** bộ test đầy đủ và hướng dẫn chạy nằm trong [`docs/api-test/`](docs/api-test/API-TEST-GUIDE.md)

Tất cả API đều có tiền tố `/api/v1`. Trừ `/api/v1/auth/**` và VNPay callback, mọi API đều cần header:

```
Authorization: Bearer <token>
```

| Nhóm | Đường dẫn |
|---|---|
| Xác thực | `/api/v1/auth` |
| Hội viên (tự thao tác) | `/api/v1/members/me/...` |
| Huấn luyện viên | `/api/v1/coaches` |
| Lễ tân | `/api/v1/receptionist/...` |
| Quản lý | `/api/v1/users`, `/api/v1/roles`, `/api/v1/permissions`, `/api/v1/manager/...`, `/api/v1/admin/audit-logs` |
| Danh mục | `/api/v1/packages`, `/api/v1/subjects`, `/api/v1/rooms`, `/api/v1/classes` |
| Thanh toán VNPay | `/api/v1/payments/vnpay` |

Lỗi nghiệp vụ được trả về dạng JSON, có mã lỗi riêng (ví dụ `ERR_CLASS_001`). Danh sách mã lỗi nằm trong `enums/ErrorCode.java`.

## Job tự động

Cấu hình trong `application.properties`, múi giờ `Asia/Ho_Chi_Minh`:

| Job | Lịch chạy mặc định | Nội dung |
|---|---|---|
| Nhắc gói sắp hết hạn | `0 0 8 * * *` (8:00 mỗi ngày) | Nhắc hội viên có gói hết hạn trong `subscription-days-before` ngày tới (mặc định 3) và chưa gia hạn |
| Nhắc lịch học | `0 */15 * * * *` (mỗi 15 phút) | Nhắc hội viên có lớp bắt đầu trong `class-hours-before` giờ tới (mặc định 24) |

Mỗi gói và mỗi lượt đăng ký lớp chỉ được nhắc **một lần**: hệ thống ghi lại thời điểm đã nhắc vào cột `expiry_reminder_sent_at` / `reminder_sent_at`. Khi test, có thể tạm đổi cron thành `0 * * * * *` để job chạy mỗi phút.

## Quy trình làm việc với Git

- Làm việc trên nhánh `hotfix` (hoặc nhánh `fix/...`, `feature/...` tách từ `main`), sau đó mở Pull Request vào `main`. **Không push thẳng lên `main`.**
- Message commit theo dạng `fix: ...`, `feat: ...`, `chore: ...`, mô tả đúng nội dung thay đổi.
- Không commit `application-local.properties` hay bất kỳ secret nào.
- Sau khi pull code mới, nhớ **restart backend** trước khi test.
