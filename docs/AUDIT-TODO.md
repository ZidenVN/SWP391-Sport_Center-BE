# AUDIT TODO (audit ngày 02/11/2026, cập nhật lại sau PR #73)

Kết quả audit toàn bộ backend trên `main`. Mỗi mục gồm: chỗ sửa, lỗi, cách sửa, và điều kiện coi là xong.

**Cập nhật lần 2 (sau PR #71–73):**
- **Đã xong 6 mục:** 5, 6, 7, 8, 9, 16.
- **Còn mở:** 14 mục cũ.
- **Mới thêm:** mục 21–23, sinh ra từ code chuyên môn HLV. Mục 21 chặn việc gán HLV, phải làm trước khi chạy bản mới.
- **Đánh số:** số thứ tự giữ nguyên như cũ, để ai đã nhận mục nào vẫn tra được.

**Tải giả định:** một trung tâm, vài trăm hội viên, chục nhân viên, 1 server trên Railway, pool kết nối DB mặc định là 10.

**Mức độ:** 🔴 phải sửa · 🟠 nên sửa · 🟡 làm thêm khi rảnh

**Người làm:** A = gói, membership, thanh toán, check-in · B = lớp, buổi học, báo cáo · C = HLV, đăng ký, đổi lớp. "Chưa giao" = phần đăng nhập / AI, trưởng nhóm gán người.

## Bảng tổng hợp

| # | Mức  | Lỗi | Người làm | Xong |
|---|------|---|---|---|
| 1 | done | Đăng nhập Google chiếm được tài khoản bất kỳ | Chưa giao | [ ] |
| 2 | done | Mua gói online làm mất đơn đang chờ ở quầy | A | [ ] |
| 3 | done | Trả tiền VNPay xong mà đóng tab thì không nhận được gói | A | [ ] |
| 4 | done | Trợ lý AI giữ kết nối DB trong lúc chờ Gemini, có thể treo cả app | Chưa giao | [ ] |
| 5 | 🔴   | Dừng lớp không gửi thông báo cho ai | B | [x] PR #71 |
| 6 | 🔴   | Huỷ được lớp đang học dở | B | [x] PR #71 |
| 7 | 🔴   | Lớp dư 1 ngày học | B | [x] PR #71 |
| 8 | 🔴   | Tạo được lớp mà không ai đăng ký được | B | [x] PR #71 |
| 9 | 🔴   | HLV nào cũng gán được vào môn nào | C | [x] PR #72 |
| 10 | 🔴   | Báo cáo doanh thu bỏ sót tiền khi hoàn một phần | B | [ ] |
| 11 | 🔴   | Deploy lên DB production sẽ làm sập trang gói | Cả team | [ ] |
| 21 | 🔴   | 🆕 HLV cũ chưa có môn trong `coach_subjects`, nên bị chặn khi gán lớp | C | [ ] |
| 12 | done | Check-in trả "gói đang hiệu lực" lẫn gói chưa dùng được | A | [ ] |
| 13 | done | Logic tính tiền hoàn chưa có test | A | [ ] |
| 14 | 🟠   | Đăng ký và đổi lớp chưa có test | C | [ ] |
| 15 | done | Code thừa: 6 mã lỗi cũ, 5 dòng cấu hình | A | [ ] |
| 22 | 🟠   | 🆕 Bỏ môn của HLV không xét các buổi HLV đó dạy thay | C | [ ] |
| 16 | 🟡   | Dừng được cả lớp chưa bắt đầu | B | [x] PR #71 |
| 17 | done | Lỗi đăng nhập Google để lộ chi tiết bên trong | Chưa giao | [ ] |
| 18 | done | Bấm "Mua" 2 lần cùng lúc tạo ra 2 đơn chờ | A | [ ] |
| 19 | 🟡   | Giới hạn câu hỏi AI mỗi ngày có thể bị vượt | Chưa giao | [ ] |
| 20 | done | Gói lớp đã hết hạn vẫn hiện "lớp đang học" | A | [ ] |
| 23 | 🟡   | 🆕 Hai lỗi khác nhau dùng chung mã `ERR_CLASS_400_DURATION` | B | [ ] |

**Thứ tự nên làm:**
1. Mục 21 trước khi chạy bản `main` mới.
2. Mục 1 và 4: chỉ vài dòng nhưng rủi ro lớn nhất.
3. Mục 2 và 3: liên quan tới tiền.
4. Mục 10.
5. Mục 11 cùng mục 21 trước khi deploy.
6. Các mục còn lại.

---

## 🔴 Phải sửa

### 1. Đăng nhập Google chiếm được tài khoản bất kỳ, kể cả Manager
- **File:** `controller/AuthController.java`, hàm `loginWithGoogle`
- **Lỗi:** Code lấy email trong token Google rồi đăng nhập vào tài khoản cùng email, nhưng **không kiểm tra Google đã xác minh email đó chưa** (`email_verified`). Một người tạo tài khoản Google gắn với `manager@sportcenter.com` mà không sở hữu hộp thư đó vẫn đăng nhập được với quyền Manager.
- **Sửa:** ngay sau dòng `GoogleIdToken.Payload payload = idToken.getPayload();`
  ```java
  if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
      return ResponseEntity.status(401).body("Email Google chưa được xác minh");
  }
  ```
- **Xong khi:** token Google có `email_verified=false` thì nhận 401; tài khoản Gmail bình thường vẫn đăng nhập được.

### 2. Mua gói online làm mất đơn đang chờ ở quầy
- **File:** `service/impl/VNPayServiceImpl.java`, vòng lặp huỷ đơn `PENDING` trong hàm tạo link thanh toán
- **Lỗi:** Vòng lặp huỷ **mọi** đơn chờ quá 15 phút, kể cả đơn **tiền mặt** đang chờ khách ra quầy trả. Đường mua ở quầy (`SubscriptionServiceImpl.subscribe`) thì chỉ huỷ đơn **VNPay** quá hạn. Cùng một việc nhưng hai chỗ áp hai quy tắc khác nhau.
  - Ví dụ: 9h khách đặt gói tiền mặt, 10h mua thêm gói online, thì đơn 9h bị huỷ ngầm. Khách mang tiền ra quầy, lễ tân xác nhận sẽ báo lỗi.
- **Sửa:** Chuyển vòng lặp trong `subscribe` thành 1 hàm dùng chung trong `SubscriptionService`, ví dụ `void cancelStalePendingOrThrow(Long memberId, LocalDateTime now)`. Gọi hàm này từ cả `subscribe` và `VNPayServiceImpl`, rồi xoá vòng lặp trong `VNPayServiceImpl`.
- **Xong khi:** có đơn tiền mặt đang chờ thì mua online nhận lỗi `ERR_PAYMENT_409` (giống mua ở quầy), và đơn tiền mặt vẫn còn `PENDING`.

### 3. Trả tiền VNPay xong mà đóng tab thì không nhận được gói
- **File:** `controller/VNPayController.java`, `service/impl/VNPayServiceImpl.processCallBack`
- **Lỗi:** Giao dịch chỉ được ghi "đã trả" khi **trình duyệt của khách** được VNPay chuyển về `GET /callback`. Hệ thống chưa có **IPN** (VNPay tự gọi thẳng server để báo kết quả). Khách trả tiền xong rồi đóng tab hoặc mất mạng thì gói nằm mãi ở `PENDING`, lần mua sau bị huỷ.
- **Sửa:**
  - Thêm endpoint `GET /api/v1/payments/vnpay/ipn` gọi lại đúng `processCallBack`. Hàm này đã kiểm tra chữ ký, số tiền và chống xử lý 2 lần.
  - Trả về đúng định dạng VNPay yêu cầu: `{"RspCode":"00","Message":"Confirm Success"}` khi thành công; `97` khi sai chữ ký; `04` khi sai số tiền; `02` khi giao dịch đã xử lý.
  - Mở endpoint trong `SecurityConfig` (`permitAll`) giống `/callback`.
  - Khai báo URL IPN trên trang quản trị VNPay sandbox. URL phải là địa chỉ công khai, ví dụ domain Railway.
- **Xong khi:** thanh toán thành công rồi đóng tab ngay, giao dịch vẫn chuyển `COMPLETED` và gói chuyển `WAITING_ACTIVATION`.

### 4. Trợ lý AI giữ kết nối DB trong lúc chờ Gemini, có thể treo cả app
- **File:** `service/impl/AiChatServiceImpl.java`, hàm `chat` (khoảng dòng 56)
- **Lỗi:** Cả hàm có `@Transactional`, nên **giữ 1 kết nối DB suốt thời gian gọi Gemini**: tối đa 5 vòng, mỗi lần chờ tới 45 giây, có thử lại và có model dự phòng. Pool chỉ có 10 kết nối. 10 hội viên hỏi cùng lúc khi Gemini chậm là cả app đứng: check-in, thanh toán, đăng nhập đều phải chờ.
- **Sửa:**
  - Bỏ `@Transactional` trên `chat`. Mỗi lệnh `save()` của repository tự có transaction ngắn riêng.
  - Vì không còn dirty checking, cuối hàm đổi thành:
    ```java
    conv.setUpdatedAt(LocalDateTime.now());
    aiConversationRepository.save(conv);
    ```
- **Xong khi:** trong lúc 1 request AI đang chờ Gemini, các API khác vẫn phản hồi bình thường; lịch sử chat và `updatedAt` vẫn được lưu đúng.

### 5–9. ✅ Đã xong (PR #71, #72)
Dừng lớp có gửi thông báo, không huỷ được lớp đã bắt đầu, `endDate` không còn dư 1 ngày, thời hạn lớp phải trùng với gói đang bán, chuyên môn HLV dùng bảng `coach_subjects`. Các mục 21–23 bên dưới là phần còn sót lại sau khi sửa.

### 10. Báo cáo doanh thu bỏ sót tiền khi hoàn một phần
- **File:** `repository/PaymentRepository.java`, các query doanh thu (khoảng dòng 47–88)
- **Lỗi:** Doanh thu chỉ tính giao dịch `COMPLETED`/`SUCCESS`. Lớp dừng giữa khoá, giao dịch 780.000đ được hoàn 676.000đ thì trung tâm vẫn giữ 104.000đ. Nhưng giao dịch đã chuyển `REFUNDED`, nên báo cáo tính **0đ**.
- **Sửa:** trong các query doanh thu, thay phần tính tiền bằng:
  ```sql
  SUM(CASE WHEN p.status IN ('COMPLETED','SUCCESS') THEN p.amount
           WHEN p.status = 'REFUNDED' THEN p.amount - COALESCE(p.refundAmount, p.amount)
           ELSE 0 END)
  ```
  và mở điều kiện `WHERE` để có thêm `'REFUNDED'`.
- **Xong khi:** giao dịch 780.000đ hoàn 676.000đ làm doanh thu kỳ đó tăng đúng 104.000đ; giao dịch hoàn đủ không làm doanh thu tăng.

### 11. Deploy lên DB production sẽ làm sập trang gói
- **File:** DB production trên Railway (`ddl-auto=update` không tự sửa dữ liệu cũ)
- **Lỗi:** DB production còn gói loại `FREE` của mô hình cũ, nên `GET /packages` sẽ lỗi 500 `No enum constant PackageType.FREE`.
- **Sửa:** trước khi deploy, chọn 1 trong 2 cách:
  - **Reset DB production**, đúng như team đã thống nhất từ đầu đợt đổi mô hình; hoặc
  - Chạy SQL:
    ```sql
    UPDATE packages SET package_type = 'MEMBERSHIP', subject_id = NULL WHERE package_type = 'FREE';
    ```
    Có thể xoá thêm các cột, bảng không dùng: `subscriptions.used_sessions`, `packages.session_limit`, `package_subjects`.
  - Chạy luôn SQL của mục 21 trong cùng đợt deploy.
- **Xong khi:** sau khi deploy, `GET /packages` trên production trả 200.

### 21. 🆕 HLV cũ chưa có môn trong `coach_subjects`, nên bị chặn khi gán lớp
- **File:** `entity/Coach.java` (bảng mới `coach_subjects`), `config/DataInitializer.java` (khoảng dòng 182–192)
- **Lỗi:** `CoachSubjectGuard` giờ chỉ đọc bảng `coach_subjects`. `ddl-auto=update` tạo bảng này **rỗng** và không chuyển dữ liệu từ cột `speciality` cũ sang.
  - Mọi HLV đang có (DB local có 9 HLV) sẽ bị lỗi `COACH_NOT_TEACH_SUBJECT` khi tạo lớp, sửa lớp, đổi HLV hoặc chọn người dạy thay.
  - HLV mặc định do `DataInitializer` tạo cũng không có môn nào.
- **Sửa:**
  - Sau khi app chạy lần đầu (lúc đó bảng đã được tạo), chạy SQL trên **từng DB** (local của mỗi người và production):
    ```sql
    INSERT IGNORE INTO coach_subjects (coach_id, subject_id)
    SELECT c.coach_id, s.subject_id
    FROM coaches c JOIN subjects s ON c.speciality LIKE CONCAT('%', s.subject_name, '%');
    ```
  - Kiểm tra lại các HLV vẫn chưa có môn: `SELECT coach_id, speciality FROM coaches WHERE coach_id NOT IN (SELECT coach_id FROM coach_subjects);`. Gán tay cho họ qua `PUT /coaches/{id}` với `subjectIds`. Ví dụ HLV "General Fitness" không khớp tên môn nào nên câu SQL trên bỏ qua.
  - Trong `DataInitializer`, gán cho HLV mặc định ít nhất 1 môn, ví dụ `.subjects(Set.of(gymSubject))`. Nếu môn chưa được tạo lúc khởi động thì bỏ qua bước này và ghi chú lại.
- **Xong khi:** mọi HLV có ít nhất 1 dòng trong `coach_subjects`, và tạo lớp Gym với HLV Gym có sẵn thì thành công.

---

## 🟠 Nên sửa

### 12. Check-in trả "gói đang hiệu lực" lẫn gói chưa dùng được
- **File:** `service/impl/CheckInServiceImpl.java:84`
- **Lỗi:** `activeSubscriptions` lấy từ `getCurrentSubscriptions`, mà hàm này giờ trả cả gói `WAITING_ACTIVATION`. Gói lớp chưa đăng ký lớp vẫn hiện như đang dùng.
- **Sửa:**
  ```java
  dto.setActiveSubscriptions(subscriptionService.getCurrentSubscriptions(memberId).stream()
      .filter(s -> "ACTIVE".equals(s.getStatus())).toList());
  ```
- **Xong khi:** hội viên có gói lớp chưa đăng ký lớp check-in thì danh sách chỉ có membership.

### 13. Logic tính tiền hoàn chưa có test
- **File:** `service/impl/PaymentServiceImpl.java` (`calculateRefundAmount`, `proratedForStoppedClass`)
- **Lỗi:** Đây là các hàm quyết định hoàn bao nhiêu tiền nhưng không có test nào. Ai sửa sai công thức thì không có gì báo lỗi.
- **Sửa:** thêm `src/test/.../PaymentServiceImplTest.java` (Mockito, giống các test có sẵn của B), tối thiểu 3 case:
  - lớp bị dừng, 26/30 buổi chưa học, gói 780.000đ: hoàn **676.000đ**;
  - membership đã kích hoạt: ném `ERR_PAYMENT_005`;
  - gói lớp đang chờ: hoàn đủ.
- **Xong khi:** `./mvnw test` chạy 3 case đều đạt.

### 14. Đăng ký và đổi lớp chưa có test
- **File:** `service/impl/ClassRegistrationServiceImpl.java`
- **Lỗi:** Đây là chỗ nhiều nhánh nhất (đăng ký, huỷ, tự đổi, lễ tân đổi, đếm lượt, khoá dòng) mà không có test nào.
- **Sửa:** thêm `ClassRegistrationServiceImplTest.java`, tối thiểu:
  - lễ tân đổi khi lớp **chưa** bắt đầu: không tăng `receptionistSwitchCount`;
  - lớp **đã** bắt đầu: tăng lên 1, và lần đổi thứ 2 bị chặn bằng `REGISTRATION_SWITCH_LIMIT`;
  - đăng ký lớp khác thời hạn gói: `REGISTRATION_DURATION_MISMATCH`.
- **Xong khi:** các case trên đạt. Nên làm trước khi sửa mục C2 trong `TODO-ROLE-B-C.md`.

### 15. Code thừa
- **File:** `enums/ErrorCode.java`, `src/main/resources/application.properties`
- **Lỗi:** Có 6 mã lỗi của mô hình gói cũ không còn chỗ nào dùng (đã tìm toàn bộ project):
  - `FREE_PACKAGE_NOT_ALLOWED`, `INVALID_PACKAGE_CONFIG`, `SUBJECT_HAS_ACTIVE_FREE_PACKAGE`;
  - `NO_SESSIONS_LEFT`, `PACKAGE_NOT_ALLOW_CLASS`, `SUBJECT_NOT_COVERED`.

  Ngoài ra có 3 dòng `spring.thymeleaf.*` (project không có thư viện Thymeleaf) và 2 dòng `spring.sql.init.*` (không có file `data.sql`).
- **Sửa:** xoá khoảng 11 dòng trên.
- **Xong khi:** compile xanh và app khởi động bình thường.

### 22. 🆕 Bỏ môn của HLV không xét các buổi HLV đó dạy thay
- **File:** `repository/CoachRepository.hasOpeningClassOfSubjects`, được gọi từ `CoachServiceImpl.updateCoach` (khoảng dòng 222)
- **Lỗi:** Query chỉ xét các lớp mà HLV là **người dạy chính**. Ví dụ: HLV A được xếp dạy thay buổi Boxing ngày 10/11, quản lý bỏ môn Boxing của A vẫn thành công, và A vẫn dạy buổi đó.
- **Sửa:** thêm vào query một điều kiện `OR EXISTS`: có `ClassSession` chưa diễn ra, `substituteCoach` là HLV này, và lớp thuộc một môn bị bỏ.
- **Xong khi:** bỏ môn của một HLV đang có buổi dạy thay của môn đó thì nhận lỗi `COACH_SUBJECT_IN_USE`.

---

## 🟡 Làm thêm khi rảnh

### 16. ✅ Đã xong (PR #71)
Dừng lớp chưa bắt đầu giờ báo lỗi `CLASS_STOP_NOT_STARTED`.

### 17. Lỗi đăng nhập Google để lộ chi tiết bên trong
- **File:** `AuthController.loginWithGoogle`, khối `catch (Exception e)`
- **Lỗi:** Trả 500 kèm `e.getMessage()` cho người dùng, có thể lộ thông tin cấu hình hoặc thư viện.
- **Sửa:** `log.error("Google login lỗi", e);` rồi trả câu chung "Đăng nhập Google thất bại, vui lòng thử lại".

### 18. Bấm "Mua" 2 lần cùng lúc tạo ra 2 đơn chờ
- **File:** `SubscriptionServiceImpl.subscribe`
- **Lỗi:** Bước kiểm tra "đã có đơn chờ chưa" không khoá dòng hội viên, nên 2 request đến cùng lúc đều qua.
- **Sửa:** đổi `memberRepository.findById(memberId)` thành `memberRepository.findByIdForUpdate(memberId)`. Hàm này đã có sẵn, check-in đang dùng. Làm cùng lúc với mục 2.

### 19. Giới hạn câu hỏi AI mỗi ngày có thể bị vượt
- **File:** `AiChatServiceImpl.chat`
- **Lỗi:** Code đếm số câu đã hỏi trước, lưu câu mới sau. Hai request gửi cùng lúc khi đang ở câu thứ 29 đều qua, nên hội viên hỏi được 31 câu.
- **Sửa:** chấp nhận được vì đây chỉ là giới hạn mềm. Muốn chặt thì khoá dòng user khi đếm. Lưu ý không làm lại lỗi ở mục 4 (giữ transaction lâu).

### 20. Gói lớp đã hết hạn vẫn hiện "lớp đang học"
- **File:** `repository/SubscriptionRepository.java`, query `findLiveRegistrationsOf`
- **Lỗi:** Khoá học kết thúc thì lớp chuyển `COMPLETED`, nhưng đăng ký vẫn là `REGISTERED`, nên lịch sử gói vẫn ghi đang học lớp đó.
- **Sửa:** thêm `AND c.status IN ('OPENING', 'STOPPED')` vào query.

### 23. 🆕 Hai lỗi khác nhau dùng chung mã `ERR_CLASS_400_DURATION`
- **File:** `enums/ErrorCode.java` (khoảng dòng 129–133)
- **Lỗi:**
  - `CLASS_DURATION_INVALID` và `CLASS_DURATION_NOT_SOLD` có cùng mã lỗi, nên FE không phân biệt được.
  - `CLASS_CANCEL_STARTED` và `CLASS_STOP_NOT_STARTED` trả HTTP 422, nhưng tên mã lại ghi `400`.
- **Sửa:** đổi mã thành `ERR_CLASS_400_DURATION_NOT_SOLD`, và cho tên mã khớp với HTTP status: đổi status về 400, hoặc đổi tên mã thành `ERR_CLASS_422_...`.

---

## Đã kiểm tra và ổn
- Quyền của HLV với dữ liệu hội viên: `CoachAccessGuard` chặn đúng ở mọi endpoint.
- JWT kiểm tra phiên bản token và trạng thái tài khoản ở mỗi request.
- API key Gemini gửi qua header, không nằm trong URL. Câu hỏi AI giới hạn 2000 ký tự.
- Múi giờ JVM được đặt về `Asia/Ho_Chi_Minh`.
- Callback VNPay có kiểm tra chữ ký và số tiền.

## Chưa kiểm tra
Lần 2: chưa chạy `./mvnw test` trên `main`; FE chưa được kiểm tra xem đã gửi `subjectIds` thay cho `specialty` chưa.
Cấu trúc bên trong WebSocket, chi tiết `AuditLogAspect`, SupportTicket, toàn bộ FE. Chưa chạy test tải, chưa kiểm tra trên DB production.
