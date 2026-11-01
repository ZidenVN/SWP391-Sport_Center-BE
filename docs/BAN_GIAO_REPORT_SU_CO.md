# TÀI LIỆU BÀN GIAO & HƯỚNG DẪN KIỂM THỬ MODULE REPORT SỰ CỐ (ISSUE REPORT)

- **Dự án**: Sports Center Management System (SWP391 - FA26)
- **Hạng mục**: Item 6 - Report sự cố (IssueReport) & Phân luồng tiếp nhận Lễ tân / Quản lý
- **Người thực hiện**: Nguyễn Phú Thịnh (`ThinhNP`)
- **Nhánh Git**: `thinh`
- **Ngày bàn giao**: 01/11/2026

---

## 1. TỔNG QUAN HẠNG MỤC (ITEM 6)

Triển khai trọn vẹn nghiệp vụ tiếp nhận và xử lý sự cố trong trung tâm thể thao theo đúng ma trận phân quyền yêu cầu từ ban quản trị:
1. **Người gửi báo cáo**: Hội viên (Member), Huấn luyện viên (Coach), Lễ tân (Receptionist) và Quản lý (Center Manager).
2. **Hình ảnh đính kèm**: Hỗ trợ tải lên ảnh minh chứng sự cố lưu trữ tự động trên Cloudinary (thư mục `sports-center/reports`). Hỗ trợ cả endpoint nhận `multipart/form-data` và endpoint `application/json` thuần.
3. **Phân luồng tiếp nhận tự động (Routing Matrix)**: Tự động phân định bàn tiếp nhận (`RECEPTION` hoặc `MANAGER`) ngay khi tạo dựa theo danh mục sự cố.
4. **Quyền tự huỷ**: Người tạo được phép huỷ báo cáo (`CANCELLED`) khi báo cáo còn ở trạng thái `OPEN`.
5. **Tiếp nhận & Xử lý**: Lễ tân và Quản lý chuyển đổi trạng thái (`IN_PROGRESS`, `RESOLVED`, `REJECTED`) kèm ghi chú giải quyết (`response`).
6. **Chuyển tiếp (Escalate)**: Lễ tân có thể chuyển báo cáo từ bàn `RECEPTION` lên bàn `MANAGER` kèm lý do.
7. **Thông báo Realtime**: Đẩy sự kiện qua hệ thống WebSocket/STOMP (`/user/queue/notifications`) và lưu database cho các bên liên quan khi: tạo sự cố, cập nhật trạng thái, và chuyển tiếp lên cấp trên.

---

## 2. MA TRẬN PHÂN LUỒNG TIẾP NHẬN (ROUTING MATRIX)

| Người Gửi | Danh Mục Sự Cố (`category`) | Nhãn Hiển Thị (`label`) | Bàn Tiếp Nhận Ban Đầu (`desk`) | Thẩm Quyền Xử Lý |
| :--- | :--- | :--- | :--- | :--- |
| **Hội viên** | `MEMBER_COACH_CLASS` | Phản ánh HLV / Lớp học | `MANAGER` | Quản lý xử lý trực tiếp |
| **Hội viên** | `MEMBER_FACILITY` | Cơ sở vật chất / Thiết bị hỏng | `RECEPTION` | Lễ tân xử lý (Có thể chuyển lên Manager) |
| **Hội viên** | `MEMBER_HYGIENE` | Vệ sinh phòng tập / Tiện ích | `RECEPTION` | Lễ tân xử lý (Có thể chuyển lên Manager) |
| **Hội viên** | `MEMBER_PACKAGE_PAYMENT`| Thắc mắc Gói tập / Thanh toán | `RECEPTION` | Lễ tân xử lý (Có thể chuyển lên Manager) |
| **Hội viên** | `MEMBER_OTHER` | Ý kiến đóng góp khác | `RECEPTION` | Lễ tân xử lý (Có thể chuyển lên Manager) |
| **HLV** | `COACH_ROOM_EQUIPMENT` | Thiết bị trong phòng hỏng | `MANAGER` | Quản lý xử lý |
| **HLV** | `COACH_SESSION_INCIDENT`| Sự cố trong buổi dạy | `MANAGER` | Quản lý xử lý |
| **HLV** | `COACH_ROOM_ISSUE` | Phòng học có vấn đề | `MANAGER` | Quản lý xử lý |
| **HLV** | `COACH_OTHER` | Vấn đề khác của HLV | `MANAGER` | Quản lý xử lý |
| **Lễ tân** | `RECEPTION_FACILITY` | Sự cố CSVC nghiêm trọng | `MANAGER` | Quản lý xử lý |
| **Lễ tân** | `RECEPTION_SYSTEM_PAYMENT` | Lỗi hệ thống / Giao dịch | `MANAGER` | Quản lý xử lý |
| **Lễ tân** | `RECEPTION_MEMBER_VIOLATION`| Hội viên vi phạm nội quy | `MANAGER` | Quản lý xử lý |
| **Lễ tân** | `RECEPTION_OTHER` | Báo cáo nội bộ khác | `MANAGER` | Quản lý xử lý |

> **Ràng buộc bảo mật danh mục**: Hội viên chỉ được chọn danh mục `MEMBER_*`, HLV chỉ được chọn `COACH_*`, Lễ tân chỉ được chọn `RECEPTION_*`. Center Manager có quyền tạo bất kỳ danh mục nào. Nếu gửi sai -> Trả mã lỗi `REPORT_CATEGORY_NOT_ALLOWED` (HTTP 400).

---

## 3. DANH SÁCH 7 API ENDPOINTS

Base URL: `/api/v1/issue-reports`

### 1a. Gửi báo cáo kèm ảnh (Multipart Form Data)
- **Endpoint**: `POST /api/v1/issue-reports`
- **Content-Type**: `multipart/form-data`
- **Quyền**: `SUBMIT_ISSUE_REPORT` (Member, Coach, Receptionist, Manager)
- **Request Part**:
  - `data` (application/json):
    ```json
    {
      "category": "MEMBER_FACILITY",
      "title": "Máy chạy bộ số 3 phòng Cardio bị kẹt băng chuyền",
      "content": "Băng chuyền rung lắc mạnh và phát ra tiếng ồn lớn khi tăng tốc."
    }
    ```
  - `image` (File, tuỳ chọn): Tệp ảnh JPG/PNG minh hoạ sự cố.
- **Response**: `201 Created`

### 1b. Gửi báo cáo dạng JSON thuần (Không kèm ảnh)
- **Endpoint**: `POST /api/v1/issue-reports`
- **Content-Type**: `application/json`
- **Quyền**: `SUBMIT_ISSUE_REPORT`
- **Request Body**:
  ```json
  {
    "category": "MEMBER_COACH_CLASS",
    "title": "HLV vào lớp trễ 15 phút không báo trước",
    "content": "Buổi tập ngày 01/11 lớp Yoga 01 HLV đến muộn làm ảnh hưởng lịch tập."
  }
  ```
- **Response**: `201 Created` (Tự động phân vào desk `MANAGER`)

### 2. Lấy danh sách báo cáo của bản thân
- **Endpoint**: `GET /api/v1/issue-reports/my`
- **Quyền**: `isAuthenticated()`
- **Mô tả**: Trả về danh sách tất cả các sự cố do chính người dùng hiện tại gửi, sắp xếp mới nhất lên đầu.
- **Response**: `200 OK` (Array JSON)

### 3. Lễ tân / Quản lý tìm kiếm & lọc báo cáo
- **Endpoint**: `GET /api/v1/issue-reports`
- **Quyền**: `HANDLE_ISSUE_REPORT` (Receptionist, Center Manager)
- **Query Params**:
  - `desk`: `RECEPTION` hoặc `MANAGER` *(Lễ tân bị ép buộc cố định ở RECEPTION, nếu truyền MANAGER sẽ bị chặn HTTP 403 `REPORT_DESK_ACCESS_DENIED`)*
  - `status`: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`, `CANCELLED`
  - `category`: Enum danh mục
  - `fromDate`, `toDate`: Định dạng `YYYY-MM-DD`
  - `keyword`: Tìm theo từ khoá trong tiêu đề hoặc nội dung
  - `page`: Trang (mặc định 0), `size`: Số phần tử (mặc định 20)
- **Response**: `200 OK` (Spring Page JSON)

### 4. Xem chi tiết 1 báo cáo sự cố
- **Endpoint**: `GET /api/v1/issue-reports/{id}`
- **Quyền**: `isAuthenticated()`
- **Bảo mật**: Người gửi luôn xem được báo cáo của mình. Manager xem được tất cả. Lễ tân chỉ được xem các báo cáo thuộc bàn `RECEPTION`. Người ngoài bị chặn HTTP 403 `REPORT_ACCESS_DENIED`.
- **Response**: `200 OK`

### 5. Người gửi tự huỷ báo cáo
- **Endpoint**: `PATCH /api/v1/issue-reports/{id}/cancel`
- **Quyền**: `SUBMIT_ISSUE_REPORT`
- **Điều kiện**:
  - Chỉ người tạo báo cáo mới có quyền huỷ (chặn HTTP 403 nếu không phải chính chủ).
  - Chỉ huỷ được khi trạng thái còn là `OPEN` (nếu đã chuyển `IN_PROGRESS`/`RESOLVED`/`REJECTED` sẽ chặn HTTP 400 `REPORT_CANNOT_CANCEL`).
- **Response**: `200 OK` (`status: "CANCELLED"`)

### 6. Tiếp nhận & Đổi trạng thái kèm phản hồi
- **Endpoint**: `PATCH /api/v1/issue-reports/{id}/status`
- **Quyền**: `HANDLE_ISSUE_REPORT` (Receptionist, Center Manager)
- **Request Body**:
  ```json
  {
    "status": "RESOLVED",
    "response": "Đã cử đội kỹ thuật thay thế băng tải máy chạy bộ thành công."
  }
  ```
  *(Giá trị `status` hợp lệ: `IN_PROGRESS`, `RESOLVED`, `REJECTED`)*
- **Ràng buộc**: Lễ tân không thể can thiệp vào các báo cáo ở bàn `MANAGER` (HTTP 403). Báo cáo đã đóng (`RESOLVED`, `REJECTED`, `CANCELLED`) không thể thay đổi tiếp (HTTP 400 `REPORT_ALREADY_CLOSED`).
- **Realtime**: Tự động gửi thông báo trực tiếp đến tài khoản người gửi.
- **Response**: `200 OK`

### 7. Lễ tân chuyển tiếp báo cáo lên Quản lý (Escalate)
- **Endpoint**: `PATCH /api/v1/issue-reports/{id}/escalate`
- **Quyền**: `HANDLE_ISSUE_REPORT` (Thường dùng bởi Lễ tân)
- **Request Body** (tuỳ chọn):
  ```json
  {
    "reason": "Hội viên yêu cầu hoàn phí dịch vụ vượt hạn mức phê duyệt của quầy Lễ tân"
  }
  ```
- **Hành vi**:
  - Chuyển `desk` từ `RECEPTION` sang `MANAGER`.
  - Nối vết lý do vào trường `response`: `[Chuyển lên Manager bởi <Tên_Lễ_Tân>]: <Lý_do>`.
  - Bắn thông báo Realtime cho Quản lý trung tâm và thông báo cho Hội viên người gửi.
- **Ràng buộc**: Không thể escalate nếu báo cáo vốn dĩ đã ở bàn `MANAGER` (HTTP 400 `REPORT_CANNOT_ESCALATE`) hoặc báo cáo đã đóng (HTTP 400 `REPORT_ALREADY_CLOSED`).
- **Response**: `200 OK`

---

## 4. KỊCH BẢN KIỂM THỬ THỰC TẾ TRÊN SWAGGER

Mở Swagger UI tại: `http://localhost:8080/swagger-ui/index.html`

### Kịch bản 1: Hội viên gửi sự cố Cơ sở vật chất & Lễ tân tiếp nhận
1. Đăng nhập với tài khoản Hội viên (Role `MEMBER`) -> Lấy JWT token dán vào Swagger `Authorize`.
2. Gọi `POST /api/v1/issue-reports` (Endpoint JSON) với nội dung:
   ```json
   {
     "category": "MEMBER_FACILITY",
     "title": "Bóng đèn phòng tập Boxing bị chập chờn",
     "content": "Bóng đèn nhấp nháy liên tục gây chói mắt học viên."
   }
   ```
3. **Kết quả mong đợi**: HTTP 201 Created. `desk` tự động là `RECEPTION`, `status` là `OPEN`.
4. Đăng nhập tài khoản Lễ tân -> Gọi `GET /api/v1/issue-reports?desk=RECEPTION` -> Thấy báo cáo vừa tạo xuất hiện trên đầu.
5. Lễ tân gọi `PATCH /api/v1/issue-reports/{id}/status` với:
   ```json
   {
     "status": "IN_PROGRESS",
     "response": "Lễ tân đã ghi nhận và gọi thợ điện kiểm tra"
   }
   ```
6. **Kết quả**: HTTP 200 OK, `handledByName` cập nhật tên Lễ tân, Hội viên nhận thông báo realtime.

### Kịch bản 2: Hội viên phản ánh HLV -> Đi thẳng lên Bàn Quản lý
1. Hội viên gọi `POST /api/v1/issue-reports` với:
   ```json
   {
     "category": "MEMBER_COACH_CLASS",
     "title": "HLV đổi bài tập quá nặng không thông báo",
     "content": "Bài tập vượt quá giáo trình lớp cơ bản."
   }
   ```
2. **Kết quả**: HTTP 201 Created, `desk` tự động gán là `MANAGER`.
3. Đăng nhập Lễ tân -> Thử gọi `GET /api/v1/issue-reports/{id}` hoặc `PATCH /api/v1/issue-reports/{id}/status` -> **Bị chặn HTTP 403 `REPORT_DESK_ACCESS_DENIED`**.
4. Đăng nhập Quản lý (Role `CENTER_MANAGER`) -> Xem và xử lý bình thường.

### Kịch bản 3: Người gửi tự huỷ sự cố khi còn OPEN
1. Hội viên gửi báo cáo nhầm danh mục.
2. Khi trạng thái đang `OPEN`, gọi `PATCH /api/v1/issue-reports/{id}/cancel`.
3. **Kết quả**: HTTP 200 OK, `status` chuyển thành `CANCELLED`.
4. Thử gọi huỷ lại lần nữa -> Báo lỗi HTTP 400 `REPORT_CANNOT_CANCEL`.
5. Đăng nhập tài khoản Member khác thử huỷ -> Báo lỗi HTTP 403 `REPORT_ACCESS_DENIED`.

### Kịch bản 4: Lễ tân chuyển tiếp sự cố lên Quản lý (Escalate)
1. Hội viên gửi sự cố `MEMBER_PACKAGE_PAYMENT` (thuộc bàn `RECEPTION`).
2. Lễ tân đăng nhập, nhận thấy vấn đề vượt thẩm quyền giải quyết.
3. Lễ tân gọi `PATCH /api/v1/issue-reports/{id}/escalate`:
   ```json
   {
     "reason": "Hội viên yêu cầu hoàn tiền do chuyển nơi công tác, cần Manager ký duyệt"
   }
   ```
4. **Kết quả**: HTTP 200 OK. `desk` chuyển sang `MANAGER`. Trường `response` ghi nhận vết chuyển tiếp.
5. Quản lý và Hội viên cùng nhận được thông báo Realtime WebSocket.

---

## 5. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG (UNIT TEST)

Toàn bộ logic nghiệp vụ được bảo vệ bởi bộ Unit Test tự động viết bằng JUnit 5 & Mockito tại file:
`src/test/java/com/fptu/swp391/sportscentermanager/service/impl/IssueReportServiceImplTest.java`

- **Số lượng test cases**: 23 bài test chuyên sâu
- **Tỷ lệ vượt qua**: **100% Pass** (23/23 tests, 0 failures, 0 errors)
- **Tổng số tests toàn dự án**: 37 tests (bao gồm CoachSubjectGuard, ClassSession, SportClass, IssueReport) -> **100% Pass**

---

## 6. PHÂN TÍCH RỦI RO KỸ THUẬT & CÁC TRƯỜNG TIỀM ẨN NGUY HIỂM (EDGE CASES)

Theo yêu cầu kiểm định chất lượng, dưới đây là các điểm cần lưu ý khi đưa vào vận hành thực tế:

### 1. Rủi ro tải ảnh lớn qua Multipart (`MultipartFile image`)
- **Vấn đề**: Người dùng có thể upload ảnh độ phân giải cực lớn (4K/8K, dung lượng > 10MB) hoặc upload các tệp nguy hiểm không phải hình ảnh.
- **Giải pháp bảo vệ đã có**:
  - Sử dụng Cloudinary Storage (`sports-center/reports`) với cơ chế upload trực tiếp lên đám mây, không lưu tạm file rác trên ổ cứng máy chủ.
  - Xử lý exception an toàn (`try-catch` trong `uploadReportImage`), nếu Cloudinary lỗi sẽ trả về exception ứng dụng chuẩn hoá mà không làm sập tiến trình.
- **Khuyến nghị Frontend**: Nên kiểm tra dung lượng ảnh ở phía client (giới hạn <= 5MB) và chỉ cho phép đuôi file ảnh (`image/jpeg`, `image/png`, `image/webp`).

### 2. Xung đột trạng thái đồng thời (Concurrent Status Updates)
- **Vấn đề**: Hai lễ tân cùng mở 1 báo cáo và cùng lúc bấm xử lý hoặc một bên bấm Escalate, một bên bấm Resolve.
- **Giải pháp bảo vệ**:
  - `IssueReportServiceImpl` kiểm tra trạng thái trước khi lưu: nếu báo cáo đã ở trạng thái đóng (`RESOLVED`, `REJECTED`, `CANCELLED`), mọi hành động cập nhật tiếp theo đều bị chặn ngay lập tức với lỗi `REPORT_ALREADY_CLOSED`.
  - Tất cả các method thay đổi trạng thái đều được bọc trong `@Transactional`.

### 3. Nguy cơ spam tạo báo cáo sự cố
- **Vấn đề**: Một hội viên có thể gửi hàng chục báo cáo sự cố liên tục trong thời gian ngắn (DDoS nghiệp vụ / spam notification).
- **Khuyến nghị nâng cấp tương lai**: Có thể bổ sung rate-limiting (ví dụ: tối đa 5 report / phút cho mỗi `userId`) qua Bucket4j hoặc Redis nếu trung tâm mở rộng quy mô lớn.

### 4. Tương thích WebSocket Notification khi mạng chập chờn
- **Vấn đề**: Nếu người dùng mất kết nối WebSocket lúc đang thao tác, thông báo đẩy có thể không hiển thị tức thời trên trình duyệt.
- **Cơ chế đảm bảo an toàn**: Hệ thống thông báo sử dụng kiến trúc kép: vừa lưu bản ghi notification vào cơ sở dữ liệu (`NotificationRepository`), vừa đẩy realtime qua STOMP. Do đó khi người dùng tải lại trang hoặc xem chuông thông báo, dữ liệu vẫn luôn hiển thị đầy đủ 100%.

---

## 7. KẾT LUẬN & TRẠNG THÁI BÀN GIAO

- Module Report sự cố (Item 6) đã hoàn thành xuất sắc 100% mục tiêu chức năng.
- Không phát sinh lỗi biên dịch, toàn bộ unit test chạy thành công.
- Không gây ảnh hưởng hay xung đột với các module hiện hữu khác trên hệ thống.
- Nhật ký đóng góp đã được cập nhật đầy đủ vào file Excel: `D:\FPT\SWP391\BE_CONTRIBUTION_WORKLOG_THINHNP.xlsx`.
