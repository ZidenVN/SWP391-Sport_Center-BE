# AUDIT TODO (audit ngày 02/11/2026, cập nhật lần 3)

Kết quả audit toàn bộ backend. Mỗi mục gồm: chỗ sửa, lỗi, cách sửa, và điều kiện coi là xong.

**Cập nhật lần 3 (sau nhánh `hotfix`, chưa merge vào `main`):**
- **Đã xong thêm 10 mục của A:** 1, 2, 3, 4, 12, 13, 15, 17, 18, 20. Mỗi mục đã được test qua API thật hoặc bằng unit test. Bộ test hiện có 47/47 test đạt.
- **Còn mở:** 10, 11, 14, 19, 21, 22, 23.
- **Mới thêm:**
  - mục 24: API đăng ký trả lỗi sai định dạng;
  - mục 25: VNPay báo thanh toán muộn thì tiền về mà khách không có gói.
- **Đánh số:** số thứ tự giữ nguyên như cũ, để ai đã nhận mục nào vẫn tra được.

**Tải giả định:** một trung tâm, vài trăm hội viên, chục nhân viên, 1 server trên Railway, pool kết nối DB mặc định là 10.

**Mức độ:** 🔴 phải sửa · 🟠 nên sửa · 🟡 làm thêm khi rảnh

**Người làm:** A = gói, membership, thanh toán, check-in, đăng nhập · B = lớp, buổi học, báo cáo · C = HLV, đăng ký, đổi lớp.

## Bảng tổng hợp: còn mở

| # | Mức | Lỗi | Người làm | Xong |
|---|---|---|---|---|
| 21 | 🔴 | HLV cũ chưa có môn trong `coach_subjects`, nên bị chặn khi gán lớp | C | [ ] |
| 24 | 🔴 | 🆕 API đăng ký trả lỗi sai định dạng và lộ chi tiết bên trong | A | [ ] |
| 10 | 🔴 | Báo cáo doanh thu bỏ sót tiền khi hoàn một phần | B | [ ] |
| 11 | 🔴 | Deploy lên DB production sẽ làm sập trang gói | Cả team | [ ] |
| 25 | 🟠 | 🆕 VNPay báo thanh toán muộn thì tiền về mà khách không có gói | A | [ ] |
| 14 | 🟠 | Đăng ký và đổi lớp chưa có test | C | [ ] |
| 22 | 🟠 | Bỏ môn của HLV không xét các buổi HLV đó dạy thay | C | [ ] |
| 19 | 🟡 | Giới hạn câu hỏi AI mỗi ngày có thể bị vượt | A | [ ] |
| 23 | 🟡 | Hai lỗi khác nhau dùng chung mã `ERR_CLASS_400_DURATION` | B | [ ] |

**Thứ tự nên làm:**
1. **Mục 21:** đang chặn việc tạo lớp ngay trên DB local. DB local có 9 HLV nhưng `coach_subjects` có 0 dòng.
2. **Mục 24:** chỉ cần xoá code.
3. **Mục 10 và 25:** liên quan tới tiền.
4. **Mục 11:** làm cùng mục 21, trong đợt deploy.
5. Các mục còn lại.

## Bảng tổng hợp: đã xong

| # | Lỗi | Sửa ở |
|---|---|---|
| 1 | Đăng nhập Google chiếm được tài khoản bất kỳ | `hotfix` |
| 2 | Mua gói online làm mất đơn đang chờ ở quầy | `hotfix` |
| 3 | Trả tiền VNPay xong mà đóng tab thì không nhận được gói | `hotfix` |
| 4 | Trợ lý AI giữ kết nối DB trong lúc chờ Gemini | `hotfix` |
| 5 | Dừng lớp không gửi thông báo cho ai | PR #71 |
| 6 | Huỷ được lớp đang học dở | PR #71 |
| 7 | Lớp dư 1 ngày học | PR #71 |
| 8 | Tạo được lớp mà không ai đăng ký được | PR #71 |
| 9 | HLV nào cũng gán được vào môn nào | PR #72 |
| 12 | Check-in trả lẫn gói chưa dùng được | `hotfix` |
| 13 | Logic tính tiền hoàn chưa có test | `hotfix` |
| 15 | Code thừa: 6 mã lỗi cũ, 5 dòng cấu hình | `hotfix` |
| 16 | Dừng được cả lớp chưa bắt đầu | PR #71 |
| 17 | Lỗi đăng nhập Google để lộ chi tiết bên trong | `hotfix` |
| 18 | Bấm "Mua" 2 lần cùng lúc tạo ra 2 đơn chờ | `hotfix` |
| 20 | Gói lớp đã hết hạn vẫn hiện "lớp đang học" | `hotfix` |

Tóm tắt những gì đã sửa ở `hotfix` (để người review PR biết cần xem gì):
- **Mục 1, 17:**
  - kiểm tra `email_verified`;
  - khối `catch` chỉ ghi log, trả câu thông báo chung;
  - lấy tên từ đúng claim `given_name` / `family_name`.
- **Mục 2, 18:**
  - luật huỷ đơn chờ dồn về một hàm `SubscriptionService.cancelStalePendingOrThrow`;
  - `subscribe` và `createPaymentUrl` khoá member bằng `findByIdForUpdate`.
- **Mục 3:**
  - thêm `GET /api/v1/payments/vnpay/ipn` và mở `permitAll` cho endpoint này;
  - có `rspCode` cho mọi nhánh;
  - `processCallBack` khoá dòng payment, nên IPN và return URL tới cùng lúc chỉ được xử lý 1 lần.
  - **Còn thiếu:** khai báo URL IPN trên trang VNPay sandbox. Việc này cần người giữ tài khoản.
- **Mục 4:**
  - bỏ `@Transactional` ở `chat`;
  - conversation mới chỉ được lưu sau khi Gemini trả lời xong.
- **Mục 12:** `activeSubscriptions` chỉ lấy gói `ACTIVE`.
- **Mục 13:** thêm `PaymentServiceImplTest` với 7 case.
- **Mục 15:** xoá 6 mã lỗi và 5 dòng cấu hình không dùng.
- **Mục 20:** `findLiveRegistrationsOf` lọc thêm `c.status IN ('OPENING','STOPPED')`.
- **Ngoài danh sách:** sửa 3 test của B trong `SportClassServiceImplTest`. HLV giả trong test chưa có môn sau PR #72, nên các test này đang fail sẵn trên `main`.

---

## 🔴 Phải sửa

### 21. HLV cũ chưa có môn trong `coach_subjects`, nên bị chặn khi gán lớp
- **File:** `entity/Coach.java` (bảng `coach_subjects`), `config/DataInitializer.java` (khoảng dòng 182–192)
- **Lỗi:**
  - `CoachSubjectGuard` chỉ đọc bảng `coach_subjects`.
  - `ddl-auto=update` tạo bảng này **rỗng** và không chuyển dữ liệu từ cột `speciality` cũ sang.
  - DB local có 9 HLV nhưng 0 dòng `coach_subjects`. Vì vậy tạo lớp, sửa lớp, đổi HLV hoặc chọn người dạy thay đều báo `COACH_NOT_TEACH_SUBJECT`.
  - HLV mặc định do `DataInitializer` tạo cũng không có môn nào.
- **Sửa:**
  - Chạy SQL trên **từng DB**: DB local của mỗi người và production.
    ```sql
    INSERT IGNORE INTO coach_subjects (coach_id, subject_id)
    SELECT c.coach_id, s.subject_id
    FROM coaches c JOIN subjects s ON c.speciality LIKE CONCAT('%', s.subject_name, '%');
    ```
  - Kiểm tra các HLV vẫn chưa có môn:
    ```sql
    SELECT coach_id, speciality FROM coaches WHERE coach_id NOT IN (SELECT coach_id FROM coach_subjects);
    ```
    Gán tay cho các HLV này qua `PUT /coaches/{id}` với `subjectIds`. Ví dụ HLV "General Fitness" không khớp tên môn nào, nên câu SQL trên bỏ qua.
  - Trong `DataInitializer`, gán cho HLV mặc định ít nhất 1 môn. Nếu lúc khởi động môn chưa được tạo thì ghi chú lại để gán tay.
- **Xong khi:** mọi HLV có ít nhất 1 dòng trong `coach_subjects`, và tạo lớp Gym với một HLV Gym có sẵn thì thành công.

### 24. 🆕 API đăng ký trả lỗi sai định dạng và lộ chi tiết bên trong
- **File:** `controller/AuthController.java`, hàm `registerUser` (khoảng dòng 89–105)
- **Lỗi:** cả hàm được bọc trong `try { ... } catch (RuntimeException e) { return badRequest().body(e.getMessage()); }`. Có 3 vấn đề:
  - **Sai định dạng:** email trùng (`EMAIL_ALREADY_TAKEN`) bị trả về dạng chữ, mã 400. Đúng ra phải là JSON `{code, message}` của `GlobalExceptionHandler`, nên FE không đọc được `code`.
  - **Lộ chi tiết:** lỗi DB, ví dụ 2 người đăng ký cùng số điện thoại cùng lúc, làm lộ câu SQL ra ngoài. Đây là cùng loại lỗi với mục 17.
  - **Code thừa:** khối `User user = new User(); ... passwordEncoder.encode(...)` tạo xong rồi bỏ đi. Mỗi lần đăng ký tốn thêm một lần băm BCrypt, khoảng 100ms.
- **Sửa:** xoá `try/catch` và khối tạo `User` thừa, chỉ giữ:
  ```java
  return ResponseEntity.ok(userService.registerUser(requestDTO));
  ```
  `GlobalExceptionHandler` sẽ tự trả lỗi đúng định dạng. Sau khi xoá, `passwordEncoder` không còn được dùng trong `AuthController` (dòng 100 là chỗ dùng duy nhất), nên xoá luôn field ở dòng 57 và import.
- **Xong khi:**
  - đăng ký bằng email đã có thì nhận đúng HTTP status và JSON `{code: "ERR_...", message}`, giống các API khác;
  - đăng ký bình thường vẫn thành công.

### 10. Báo cáo doanh thu bỏ sót tiền khi hoàn một phần
- **File:** `repository/PaymentRepository.java`, 5 query doanh thu (khoảng dòng 45–93)
- **Lỗi:** doanh thu chỉ tính giao dịch `COMPLETED` / `SUCCESS`. Ví dụ lớp dừng giữa khoá, giao dịch 780.000đ được hoàn 676.000đ, nên trung tâm vẫn giữ 104.000đ. Nhưng giao dịch đã chuyển sang `REFUNDED`, nên báo cáo tính **0đ**.
- **Sửa:**
  - Trong các query doanh thu, thay phần tính tiền bằng:
    ```sql
    SUM(CASE WHEN p.status IN ('COMPLETED','SUCCESS') THEN p.amount
             WHEN p.status = 'REFUNDED' THEN p.amount - COALESCE(p.refundAmount, p.amount)
             ELSE 0 END)
    ```
  - Mở thêm `'REFUNDED'` trong điều kiện `WHERE`.
  - Query native (`getRevenueByDay`) dùng tên cột `refund_amount`.
  - Query đếm số giao dịch (`cntSuccessfulPaymentsBetween`) nên giữ nguyên. Đó là số đơn bán được, không phải số tiền.
- **Xong khi:** giao dịch 780.000đ hoàn 676.000đ làm doanh thu kỳ đó tăng đúng 104.000đ, còn giao dịch hoàn đủ không làm doanh thu tăng.

### 11. Deploy lên DB production sẽ làm sập trang gói
- **File:** DB production trên Railway (`ddl-auto=update` không tự sửa dữ liệu cũ)
- **Lỗi:** DB production còn gói loại `FREE` của mô hình cũ, nên `GET /packages` sẽ lỗi 500 `No enum constant PackageType.FREE`. DB local đã sạch.
- **Sửa:** trước khi deploy, chọn 1 trong 2 cách:
  - **Reset DB production**, đúng như team đã thống nhất từ đầu đợt đổi mô hình; hoặc
  - Chạy SQL:
    ```sql
    UPDATE packages SET package_type = 'MEMBERSHIP', subject_id = NULL WHERE package_type = 'FREE';
    ```
    Có thể xoá thêm các cột và bảng không dùng: `subscriptions.used_sessions`, `packages.session_limit`, `package_subjects`.
  - Chạy luôn SQL của mục 21 trong cùng đợt deploy.
- **Xong khi:** sau khi deploy, `GET /packages` trên production trả 200 và tạo được lớp mới.

---

## 🟠 Nên sửa

### 25. 🆕 VNPay báo thanh toán muộn thì tiền về mà khách không có gói
- **File:** `service/impl/VNPayServiceImpl.processCallBack` (nhánh "payment không còn PENDING"), `SubscriptionServiceImpl.cancelStalePendingOrThrow`
- **Lỗi:** khi khách tạo link mới, link VNPay cũ đã quá 15 phút bị huỷ (`CANCELLED`). Còn IPN hoặc callback nhận được payment không còn `PENDING` thì trả `02` và **không làm gì thêm**. Ví dụ:
  - Khách trả tiền ở phút 14:59.
  - IPN của VNPay đến muộn, vào phút 16.
  - Trong lúc đó khách bấm "Mua" lại, nên payment cũ đã bị huỷ.
  - IPN đến với `vnp_ResponseCode=00` nhưng chỉ nhận về `02`. Tiền đã trừ mà không có gói nào, và không ai biết.
- **Sửa:** trong nhánh "không còn PENDING" (sau bước kiểm tra chữ ký):
  ```java
  if ("CANCELLED".equals(payment.getStatus()) && "00".equals(request.getParameter("vnp_ResponseCode"))) {
      log.error("VNPay báo đã thu tiền cho giao dịch đã huỷ: paymentId={}, vnp_TransactionNo={}",
          paymentId, request.getParameter("vnp_TransactionNo"));
      // báo cho quản lý để hoàn tiền tay
  }
  ```
  Để báo cho quản lý, dùng lại `notificationService.notifyUsers` gửi tới các tài khoản `CENTER_MANAGER`. Vẫn trả `02` cho VNPay.
- **Xong khi:** tạo link, sửa payment trong DB thành `CANCELLED`, rồi gọi `/ipn` với chữ ký đúng và `vnp_ResponseCode=00`. Kết quả mong đợi: nhận `02`, log có dòng ERROR, và quản lý nhận được thông báo. Có thể sửa script `todo3_test.mjs` để test.

### 14. Đăng ký và đổi lớp chưa có test
- **File:** `service/impl/ClassRegistrationServiceImpl.java`
- **Lỗi:**
  - Đây là service có nhiều nhánh nhất: đăng ký, huỷ, tự đổi lớp, lễ tân đổi lớp, đếm lượt, khoá dòng. Vậy mà chưa có test nào.
  - PR #72 vừa đổi luật tính lượt thành `countTurn = hasStarted(...)` mà không có test nào kiểm tra.
- **Sửa:** thêm `ClassRegistrationServiceImplTest.java`. Có thể copy cách dựng mock từ `PaymentServiceImplTest`. Tối thiểu các case sau:
  - lễ tân đổi lớp khi lớp **chưa** bắt đầu: không tăng `receptionistSwitchCount`;
  - lễ tân đổi lớp khi lớp **đã** bắt đầu: tăng lên 1, và lần đổi thứ 2 bị chặn bằng `REGISTRATION_SWITCH_LIMIT`;
  - đăng ký lớp khác thời hạn gói: `REGISTRATION_DURATION_MISMATCH`.
- **Xong khi:** `./mvnw test` chạy các case trên đều đạt.

### 22. Bỏ môn của HLV không xét các buổi HLV đó dạy thay
- **File:** `repository/CoachRepository.hasOpeningClassOfSubjects`, được gọi từ `CoachServiceImpl.updateCoach` (khoảng dòng 222)
- **Lỗi:** query chỉ xét các lớp mà HLV là **người dạy chính**. Ví dụ HLV A được xếp dạy thay buổi Boxing ngày 10/11. Quản lý bỏ môn Boxing của A vẫn thành công, và A vẫn dạy buổi đó.
- **Sửa:** thêm vào query một điều kiện `OR EXISTS`: có `ClassSession` chưa diễn ra, `substituteCoach` là HLV này, và lớp thuộc một môn bị bỏ.
- **Xong khi:** bỏ môn của HLV đang có buổi dạy thay thuộc môn đó thì nhận lỗi `COACH_SUBJECT_IN_USE`.

---

## 🟡 Làm thêm khi rảnh

### 19. Giới hạn câu hỏi AI mỗi ngày có thể bị vượt
- **File:** `service/impl/AiChatServiceImpl.java`, hàm `chat` (khoảng dòng 60–64)
- **Lỗi:**
  - Code đếm số câu đã hỏi trước, rồi mới lưu câu mới sau khi Gemini trả lời.
  - Đang ở câu thứ 29 mà gửi 3 request cùng lúc thì cả 3 đều qua.
  - Sau mục 4, `chat` không còn transaction bọc ngoài, nên trường hợp này dễ xảy ra hơn.
- **Sửa:** chấp nhận được vì đây chỉ là giới hạn mềm. Ghi lại quyết định bằng comment ngay trên bước đếm:
  ```java
  // shortcut: đếm rồi mới lưu nên request song song có thể vượt vài câu; muốn chặt thì đếm bằng bảng counter có khoá riêng
  ```
  **Không** khoá dòng user trong lúc chờ Gemini, vì làm vậy sẽ quay lại đúng lỗi của mục 4.

### 23. Hai lỗi khác nhau dùng chung mã `ERR_CLASS_400_DURATION`
- **File:** `enums/ErrorCode.java` (khoảng dòng 123–126)
- **Lỗi:**
  - `CLASS_DURATION_INVALID` và `CLASS_DURATION_NOT_SOLD` có cùng mã lỗi, nên FE không phân biệt được hai trường hợp.
  - `CLASS_CANCEL_STARTED` và `CLASS_STOP_NOT_STARTED` trả HTTP 422 nhưng tên mã lại ghi `400`.
- **Sửa:**
  - Đổi mã thành `ERR_CLASS_400_DURATION_NOT_SOLD`.
  - Cho tên mã khớp với HTTP status: hoặc đổi status về 400, hoặc đổi tên mã thành `ERR_CLASS_422_...`.
  - Báo FE nếu FE đang so mã cũ.

---

## Đã kiểm tra và ổn
- Quyền của HLV với dữ liệu hội viên: `CoachAccessGuard` chặn đúng ở mọi endpoint.
- JWT kiểm tra phiên bản token và trạng thái tài khoản ở mỗi request. Tài khoản bị khoá có token cũng không gọi được API.
- Các secret (`jwt.secret`, `vnpay.hash-secret`, mật khẩu DB và mật khẩu tài khoản khởi tạo) không có giá trị mặc định trong code.
- API key Gemini gửi qua header, không nằm trong URL. Câu hỏi AI giới hạn 2000 ký tự.
- Múi giờ JVM và các job theo lịch đều dùng `Asia/Ho_Chi_Minh`.
- Job nhắc lịch học chỉ lấy buổi `SCHEDULED` của lớp `OPENING`. Khung thời gian `(from, to]` không chồng nhau, nên mỗi buổi chỉ được nhắc 1 lần.
- Đăng ký lớp khoá member, lớp và gói theo cùng một thứ tự, nên tránh được tranh chỗ cuối.

## Chưa kiểm tra
- WebSocket / STOMP, `AuditLogAspect`, SupportTicket, WorkoutPlan, upload ảnh Cloudinary.
- Toàn bộ FE, bao gồm việc FE đã gửi `subjectIds` thay cho `specialty` chưa.
- Chưa test VNPay sandbox thật (cần URL công khai), chưa chạy test tải, chưa kiểm tra trên DB production.
