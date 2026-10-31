# TÀI LIỆU BÀN GIAO CÔNG VIỆC & HƯỚNG DẪN KIỂM THỬ (ROLE B)
## Module: Khoá Học Theo Lịch Lặp Cố Định & Quản Lý Buổi Học (SportClass & ClassSession)

- **Dự án:** Sports Center Management System (SWP391)
- **Thành viên phụ trách:** Nguyễn Phú Thịnh (`ThinhNP`)
- **Vai trò:** Backend Developer (Role B)
- **Nhánh Git:** `thinh` (Commit ID: `ebbe698`)
- **Môi trường Database:** Docker MySQL Container (`localhost:3306/sportcenter_db`)
- **Tình trạng:** Đã hoàn thành 100%, 14/14 Unit Test tự động vượt qua (BUILD SUCCESS), sẵn sàng bàn giao cho Team & Frontend.

---

## 1. TỔNG QUAN BÀN GIAO

Role B phụ trách toàn bộ việc tái cấu trúc từ mô hình **"Lớp học đơn lẻ"** sang mô hình **"Khoá học lặp lịch tuần cố định sinh danh sách các buổi học"**, bảo vệ dữ liệu lịch học theo tiến độ, phân công huấn luyện viên dạy thay, hoãn buổi học, xếp lịch học bù và dừng lớp khẩn cấp.

### Các thành phần chính đã xây dựng:
1. **Entity `ClassSession` & Cập nhật `SportClass`:**
   - Mỗi `SportClass` đại diện cho một khoá học kéo dài (ví dụ: Khoá Yoga 30 ngày, học Thứ 2, 4, 6 từ 18:00 - 19:30).
   - Mỗi `ClassSession` đại diện cho 1 buổi học cụ thể trong khoá học, có trạng thái (`SCHEDULED`, `POSTPONED`, `DONE`, `CANCELLED`), loại buổi (`REGULAR`, `MAKEUP`), HLV phụ trách, phòng học và liên kết buổi bù (`makeupFor`).
2. **Kiểm tra chuyên môn HLV (`CoachSubjectGuard`):**
   - Đảm bảo HLV được phân công vào lớp hoặc dạy thay bắt buộc phải có chuyên môn phù hợp với môn học của lớp.
3. **Kiểm tra xung đột lịch nguyên tử (Atomic Conflict Checking):**
   - Tự động rà soát lịch phòng và lịch HLV cho toàn bộ các buổi học. Nếu có bất kỳ buổi nào bị trùng -> Rollback toàn bộ giao dịch, không để lại dữ liệu rác.
4. **Vòng đời cập nhật lớp học 3 Case:**
   - Case 1: Chưa ai đăng ký -> Cho phép sửa toàn bộ lịch học (tự động xoá và tái tạo lại các buổi học).
   - Case 2: Đã có đăng ký nhưng chưa bắt đầu -> Khoá sửa lịch, cho phép sửa HLV/phòng/sĩ số và tự động đồng bộ sang toàn bộ các buổi học tương lai.
   - Case 3: Lớp đã bắt đầu -> Khoá sửa lịch/HLV/phòng, chỉ cho phép tăng giảm sĩ số (>= số đã đăng ký) và đổi ảnh banner.
5. **Vòng đời buổi học (`ClassSessionController`):**
   - Tra cứu danh sách buổi học theo khoá.
   - Gán HLV dạy thay (`/substitute`).
   - Hoãn buổi học (`/postpone`).
   - Xếp lịch học bù (`/makeup-sessions`) với cơ chế tự động kéo dài ngày kết thúc khoá học nếu buổi bù vượt hạn cũ.
   - Dừng lớp khẩn cấp (`/stop`) với cơ chế tự động huỷ các buổi tương lai và gửi thông báo in-app realtime.

---

## 2. DANH SÁCH MÃ NGUỒN BÀN GIAO

| Nhóm | Tên tệp tin | Đường dẫn | Chức năng chính |
|---|---|---|---|
| **Entity** | `ClassSession.java` | `src/main/java/.../entity/ClassSession.java` | Thực thể buổi học (buổi thường/học bù, HLV, phòng, giờ, trạng thái) |
| **Entity** | `SportClass.java` | `src/main/java/.../entity/SportClass.java` | Bổ sung `startDate`, `endDate`, `durationDays`, `daysOfWeek`, `startClock`, `endClock`, `firstSessionAt` |
| **Enum** | `SessionType.java` | `src/main/java/.../enums/SessionType.java` | `REGULAR`, `MAKEUP` |
| **Enum** | `SessionStatus.java` | `src/main/java/.../enums/SessionStatus.java` | `SCHEDULED`, `POSTPONED`, `DONE`, `CANCELLED` |
| **Guard** | `CoachSubjectGuard.java` | `src/main/java/.../guard/CoachSubjectGuard.java` | Chặn phân công HLV sai chuyên môn môn học |
| **Repository**| `ClassSessionRepository.java`| `src/main/java/.../repository/ClassSessionRepository.java`| Query buổi học, kiểm tra xung đột phòng/HLV, query buổi học quanh giờ check-in |
| **DTO** | `ClassSessionResponseDTO.java`| `src/main/java/.../dto/ClassSessionResponseDTO.java`| Dữ liệu chi tiết 1 buổi học |
| **DTO** | `SubstituteCoachRequestDTO.java`| `src/main/java/.../dto/SubstituteCoachRequestDTO.java`| Body phân công HLV dạy thay |
| **DTO** | `PostponeSessionRequestDTO.java`| `src/main/java/.../dto/PostponeSessionRequestDTO.java`| Body lý do hoãn buổi học |
| **DTO** | `MakeupSessionRequestDTO.java`| `src/main/java/.../dto/MakeupSessionRequestDTO.java`| Body thông tin xếp lịch học bù |
| **DTO** | `ClassStopRequestDTO.java`| `src/main/java/.../dto/ClassStopRequestDTO.java`| Body lý do dừng lớp khẩn cấp |
| **DTO** | `SportClassRequestDTO.java`| `src/main/java/.../dto/SportClassRequestDTO.java`| Cập nhật nhận thông tin khoá học lặp lịch |
| **DTO** | `SportClassResponseDTO.java`| `src/main/java/.../dto/SportClassResponseDTO.java`| Cập nhật trả về tóm tắt lịch học trực quan |
| **Service** | `ClassSessionService.java` & Impl | `src/main/java/.../service/ClassSessionService.java`| Nghiệp vụ buổi học (dạy thay, hoãn, học bù) |
| **Service** | `SportClassService.java` & Impl | `src/main/java/.../service/SportClassService.java`| Nghiệp vụ khoá học (tạo lịch lặp, 3-case update, stop) |
| **Service** | `ReportServiceImpl.java` | `src/main/java/.../service/impl/ReportServiceImpl.java`| Đếm số buổi học diễn ra trong ngày chính xác |
| **Controller**| `ClassSessionController.java`| `src/main/java/.../controller/ClassSessionController.java`| 4 Endpoint quản lý buổi học |
| **Controller**| `SportClassController.java`| `src/main/java/.../controller/SportClassController.java`| Endpoint dừng lớp khẩn cấp `/classes/{id}/stop` |
| **Unit Test**| `CoachSubjectGuardTest.java`| `src/test/java/.../guard/CoachSubjectGuardTest.java`| 5 Test cases kiểm tra chuyên môn HLV |
| **Unit Test**| `ClassSessionServiceImplTest.java`| `src/test/java/.../service/impl/ClassSessionServiceImplTest.java`| 5 Test cases nghiệp vụ buổi học |
| **Unit Test**| `SportClassServiceImplTest.java`| `src/test/java/.../service/impl/SportClassServiceImplTest.java`| 4 Test cases tạo lịch lặp, 3 Case update, stop |

---

## 3. DANH SÁCH API ENDPOINTS BÀN GIAO

| STT | Endpoint | Method | Phân quyền | Mô tả chức năng |
|---|---|---|---|---|
| 1 | `/api/v1/classes` | `POST` | `CENTER_MANAGER` | Tạo khoá học có lịch lặp cố định, tự động sinh các `ClassSession` |
| 2 | `/api/v1/classes/{id}` | `PUT` | `CENTER_MANAGER` | Cập nhật khoá học theo 3 Case (0 đăng ký, có đăng ký chưa học, đã bắt đầu) |
| 3 | `/api/v1/classes/{id}/stop` | `PATCH` | `CENTER_MANAGER` | Dừng lớp khẩn cấp, huỷ toàn bộ buổi học tương lai, gửi thông báo |
| 4 | `/api/v1/classes/{classId}/sessions` | `GET` | `VIEW_CLASS` | Lấy danh sách toàn bộ các buổi học của lớp học theo thứ tự thời gian |
| 5 | `/api/v1/sessions/{sessionId}/substitute` | `PATCH` | `CENTER_MANAGER` | Phân công HLV dạy thay cho một buổi học cụ thể (kiểm tra chuyên môn & trùng lịch) |
| 6 | `/api/v1/sessions/{sessionId}/postpone` | `PATCH` | `CENTER_MANAGER` | Hoãn buổi học, ghi nhận lý do và gửi thông báo cho học viên |
| 7 | `/api/v1/classes/{classId}/makeup-sessions` | `POST` | `CENTER_MANAGER` | Xếp lịch dạy bù cho buổi bị hoãn, tự động gia hạn `endDate` nếu vượt hạn |

---

## 4. HƯỚNG DẪN KIỂM THỬ CHI TIẾT (STEP-BY-STEP TESTING GUIDE)

### Chuẩn bị trước khi test
- Địa chỉ Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Tài khoản Quản lý (Center Manager): `manager@sportcenter.com` / `toilamanager`
- Tài khoản Huấn luyện viên (Coach): `coach@Sportcenter.com` / `123456`
- Tài khoản Hội viên (Member): `thinhai1811@gmail.com` / `123456`
- Đăng nhập tại `POST /api/v1/auth/login` để lấy JWT Token, sau đó bấm nút **Authorize** ở góc trên Swagger UI để gắn token.

---

### KỊCH BẢN 1: Tạo khoá học lặp lịch cố định & Kiểm tra tự động sinh buổi học
1. Vào `SportClassController` -> `POST /api/v1/classes` (Multipart Form):
   - Part `data` (application/json):
     ```json
     {
       "roomId": 1,
       "subjectId": 1,
       "coachId": 3,
       "maxCapacity": 20,
       "startDate": "2026-11-05",
       "durationDays": 30,
       "daysOfWeek": "MON,WED,FRI",
       "startClock": "18:00:00",
       "endClock": "19:30:00"
     }
     ```
   - Part `image`: (Có thể để trống hoặc đính kèm ảnh).
2. **Kết quả mong đợi (HTTP 200 OK):**
   - `classId`: số ID của lớp học mới.
   - `status`: `"OPENING"`.
   - `totalSessions`: 13 (hoặc 12 tuỳ vào lịch ngày trong tháng).
   - `scheduleSummary`: `"Thứ 2, Thứ 4, Thứ 6 (18:00 - 19:30) (Từ 05/11/2026 đến 05/12/2026)"`.
3. Kiểm tra danh sách buổi học:
   - Mở `ClassSessionController` -> `GET /api/v1/classes/{classId}/sessions`.
   - **Kết quả mong đợi:** Trả về mảng JSON gồm đúng danh sách các buổi học rơi vào các ngày Thứ 2, Thứ 4, Thứ 6 trong khoảng thời gian từ `05/11/2026` đến `05/12/2026`. Mọi buổi học đều có `status = "SCHEDULED"`, `type = "REGULAR"`, `coachId = 3`, `roomId = 1`.

---

### KỊCH BẢN 2: Kiểm thử 3 Case cập nhật lớp học theo tiến độ (`PUT /classes/{id}`)

#### Case 1: Lớp chưa ai đăng ký (0 đăng ký)
- Gọi `PUT /api/v1/classes/{id}` với `daysOfWeek`: `"TUE,THU,SAT"`.
- **Kết quả mong đợi (HTTP 200 OK):** Hệ thống cho phép đổi lịch. Toàn bộ các buổi học cũ Thứ 2, 4, 6 bị xoá và tái tạo thành công danh sách các buổi học Thứ 3, 5, 7.

#### Case 2: Lớp đã có người đăng ký nhưng chưa đến ngày học
- Dùng tài khoản Member đăng ký vào lớp qua `POST /api/v1/class-registrations` (hoặc chèn 1 bản ghi `REGISTERED` trong bảng `class_registrations`).
- Dùng Manager gọi `PUT /api/v1/classes/{id}` cố tình thay đổi `startClock` từ `18:00:00` sang `09:00:00`:
  - **Kết quả mong đợi (HTTP 400 Bad Request):**
    ```json
    {
      "status": 400,
      "code": "ERR_CLASS_400_SCHEDULE_LOCKED",
      "message": "Lớp đã có người đăng ký hoặc đã bắt đầu, không được sửa lịch học!"
    }
    ```
- Gọi `PUT /api/v1/classes/{id}` nhưng giữ nguyên lịch, chỉ đổi sang Phòng học khác (`roomId = 2`) hoặc HLV khác (`coachId = 4` có cùng chuyên môn):
  - **Kết quả mong đợi (HTTP 200 OK):** Cập nhật thành công. Toàn bộ các buổi học sắp tới trong `class_sessions` tự động được đồng bộ sang phòng/HLV mới.

#### Case 3: Lớp đã bắt đầu học
- Khi thời gian thực tế đã vượt qua `firstSessionAt` (buổi học đầu tiên):
- Manager gọi `PUT /api/v1/classes/{id}` cố tình đổi HLV hoặc Phòng học:
  - **Kết quả mong đợi (HTTP 400 Bad Request):**
    ```json
    {
      "status": 400,
      "code": "ERR_CLASS_400_STARTED",
      "message": "Lớp đã bắt đầu, chỉ được sửa sĩ số và ảnh!"
    }
    ```
- Chỉ cho phép sửa `maxCapacity` (phải >= số học viên đã đăng ký) và ảnh banner.

---

### KỊCH BẢN 3: Phân công Huấn luyện viên dạy thay (`PATCH /sessions/{id}/substitute`)
1. Mở `ClassSessionController` -> `PATCH /api/v1/sessions/{sessionId}/substitute`.
2. **Trường hợp 1 - Thành công:**
   - Body:
     ```json
     {
       "coachId": 4,
       "note": "HLV chính bị cảm đột xuất"
     }
     ```
   - HLV 4 có chuyên môn dạy môn này và không trùng lịch.
   - **Kết quả:** HTTP `200 OK`, `coachId` của buổi học đổi sang 4, trường `note` ghi nhận `"HLV dạy thay: ... (HLV chính bị cảm đột xuất)"`. Học viên và HLV 4 nhận thông báo in-app.
3. **Trường hợp 2 - Chặn HLV không đúng chuyên môn:**
   - Gán HLV môn Boxing sang dạy lớp Yoga:
   - **Kết quả:** HTTP `400 Bad Request` (`ERR_CLASS_400_COACH_SUBJECT: Huấn luyện viên không có chuyên môn dạy môn học này!`).
4. **Trường hợp 3 - Chặn HLV bị trùng lịch:**
   - Gán HLV đang có buổi dạy khác trong cùng khung giờ:
   - **Kết quả:** HTTP `409 Conflict` (`ERR_COACH_409: Huấn luyện viên đã có lịch dạy trùng thời gian này!`).

---

### KỊCH BẢN 4: Hoãn buổi học (`PATCH /sessions/{id}/postpone`)
1. Mở `ClassSessionController` -> `PATCH /api/v1/sessions/{sessionId}/postpone`.
2. Body:
   ```json
   {
     "reason": "Phòng học bảo trì sàn gỗ khẩn cấp"
   }
   ```
3. **Kết quả mong đợi (HTTP 200 OK):**
   - Buổi học chuyển sang `status = "POSTPONED"`.
   - Trường `note` ghi nhận: `"Hoãn: Phòng học bảo trì sàn gỗ khẩn cấp"`.
   - Học viên và HLV phụ trách nhận thông báo hoãn buổi học.
4. **Thử hoãn lại lần nữa:**
   - Bấm Execute lại vào buổi học đã `POSTPONED` -> **Kết quả:** Báo lỗi `400 Bad Request` (`ERR_SESSION_400_POSTPONE: Chỉ được hoãn buổi học chưa diễn ra và đang được lên lịch!`).

---

### KỊCH BẢN 5: Xếp lịch dạy bù & Tự động gia hạn kết thúc khoá (`POST /classes/{id}/makeup-sessions`)
1. Mở `ClassSessionController` -> `POST /api/v1/classes/{classId}/makeup-sessions`.
2. Body:
   ```json
   {
     "postponedSessionId": 101,
     "startTime": "2026-12-10T18:00:00",
     "endTime": "2026-12-10T19:30:00",
     "roomId": 1,
     "coachId": 3,
     "note": "Dạy bù cho buổi sàn gỗ ngày 10/11"
   }
   ```
3. **Kết quả mong đợi (HTTP 200 OK):**
   - Sinh 1 buổi học mới với `type = "MAKEUP"`, `status = "SCHEDULED"`, `makeupForSessionId = 101`.
   - Học viên nhận thông báo xếp lịch học bù.
4. **Kiểm tra tự động kéo dài ngày kết thúc:**
   - Ngày bù là `10/12/2026`, vượt qua ngày kết thúc cũ của lớp là `05/12/2026`.
   - Vào `GET /api/v1/classes/{id}` -> Kiểm tra trường `endDate` của lớp đã tự động được nâng lên `2026-12-10`.

---

### KỊCH BẢN 6: Dừng lớp học khẩn cấp (`PATCH /classes/{id}/stop`)
1. Mở `SportClassController` -> `PATCH /api/v1/classes/{id}/stop`.
2. Body:
   ```json
   {
     "reason": "Trung tâm gặp sự cố lưới điện khu vực"
   }
   ```
3. **Kết quả mong đợi (HTTP 200 OK):**
   - Lớp học chuyển sang `status = "STOPPED"`.
   - Toàn bộ các buổi học sắp tới (từ thời điểm gọi API trở đi) trong `class_sessions` tự động chuyển sang `status = "CANCELLED"` kèm lý do dừng lớp trong `note`.
   - Toàn bộ học viên đã đăng ký nhận được thông báo in-app realtime về việc lớp dừng khẩn cấp.

---

### KỊCH BẢN 7: Kiểm thử tự động bằng Maven (Unit Tests)
Tại thư mục gốc của Backend, mở terminal và chạy lệnh:
```powershell
$env:JAVA_HOME = "C:\Users\Windows\.jdks\temurin-21.0.12.1"
.\mvnw.cmd test
```
**Kết quả kiểm chứng:**
- `CoachSubjectGuardTest`: 5/5 tests pass.
- `ClassSessionServiceImplTest`: 5/5 tests pass.
- `SportClassServiceImplTest`: 4/4 tests pass.
- **Tổng cộng: 14/14 tests pass 100%, thời gian chạy ~40 giây, BUILD SUCCESS.**

---

## 5. PHÂN TÍCH RỦI RO & EDGE CASES ĐÃ ĐƯỢC XỬ LÝ (RISK MANAGEMENT)

1. **Chuỗi `daysOfWeek` không chuẩn:**
   - Đã xử lý regex và parser chịu lỗi cho cả tiếng Anh full (`MONDAY`), viết tắt (`MON`), số thứ tự tiếng Việt (`2, 4, 6`), (`T2, T4, T6`). Nếu nhập chuỗi không có nghĩa -> Hệ thống chặn ném mã `CLASS_DAYS_OF_WEEK_INVALID`.
2. **Năm nhuận và chu kỳ ngày:**
   - Dùng `LocalDate.plusDays(durationDays)` chuẩn của Java Time API, đảm bảo tính chuẩn xác tuyệt đối qua các tháng 28, 29, 30, 31 ngày.
3. **Rollback toàn vẹn khi trùng lịch (Atomic Conflict Rollback):**
   - Sử dụng `@Transactional` cho `createClass` và `updateClass`. Nếu buổi học thứ $N$ bị trùng phòng/HLV thì toàn bộ các buổi học đã tính toán trước đó sẽ tự động rollback, không để lại bất kỳ bản ghi mồ côi nào trong DB.
4. **Sức chứa phòng học khi đổi phòng:**
   - Hệ thống luôn kiểm tra `maxCapacity <= room.getCapacity()` trước khi cho phép gán phòng ở bất kỳ thao tác nào.
5. **Tương thích ngược (Backward Compatibility):**
   - Các trường `startTime`, `endTime` của `SportClass` vẫn được duy trì giá trị tương ứng với buổi đầu tiên và buổi cuối cùng của khoá, đảm bảo các chức năng cũ (báo cáo, check-in cũ) không bị lỗi NullPointer.

---

## 6. LƯU Ý PHỐI HỢP VỚI ROLE A & ROLE C

1. **Gửi cho Role A (Lễ tân & Check-in):**
   - Trong `ClassSessionRepository` đã có sẵn hàm:
     ```java
     List<ClassSession> findMemberSessionAround(Long memberId, LocalDateTime minTime, LocalDateTime maxTime);
     ```
   - Role A khi viết logic check-in hội viên hết hạn Membership chỉ cần gọi hàm này truyền `[now - 30 phút, now + thời lượng buổi]` là kiểm tra được ngay hội viên có buổi học hôm nay hay không để cho phép check-in.
2. **Gửi cho Role C (HLV & Điểm danh):**
   - Màn hình điểm danh của HLV cần nhận `sessionId` để điểm danh cho từng buổi học cụ thể của lớp thay vì điểm danh chung cho cả khoá học.
3. **Múi giờ:**
   - Đảm bảo MySQL chạy múi giờ `Asia/Ho_Chi_Minh` (`+07:00`) để đồng bộ với thời gian thực của trung tâm.

---
*Tài liệu được lập ngày 31/10/2026 bởi Nguyễn Phú Thịnh (`ThinhNP`). Mọi mã nguồn và kiểm thử đã được lưu trữ trên nhánh `thinh` (commit `ebbe698`).*
