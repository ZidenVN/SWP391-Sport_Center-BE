# AUDIT TODO: 20 lỗi cần sửa (audit ngày 02/11/2026)

Kết quả audit toàn bộ backend trên `main` (tới PR #70). Mỗi mục gồm: chỗ sửa, lỗi, cách sửa, và điều kiện coi là xong.

**Tải giả định:** một trung tâm, vài trăm hội viên, chục nhân viên, 1 server trên Railway, pool kết nối DB mặc định là 10.

**Mức độ:** 🔴 phải sửa · 🟠 nên sửa · 🟡 làm thêm khi rảnh

**Người làm:** A = gói, membership, thanh toán, check-in · B = lớp, buổi học, báo cáo · C = HLV, đăng ký, đổi lớp. "Chưa giao" = phần đăng nhập / AI, trưởng nhóm gán người.

Mục 5–9 trùng với `docs/TODO-ROLE-B-C.md`. File đó có code mẫu chi tiết hơn.

## Bảng tổng hợp

| # | Mức | Lỗi | Người làm | Xong |
|---|---|---|---|---|
| 1 | 🔴 | Đăng nhập Google chiếm được tài khoản bất kỳ | Chưa giao | [ ] |
| 2 | 🔴 | Mua gói online làm mất đơn đang chờ ở quầy | A | [ ] |
| 3 | 🔴 | Trả tiền VNPay xong mà đóng tab thì không nhận được gói | A | [ ] |
| 4 | 🔴 | Trợ lý AI giữ kết nối DB trong lúc chờ Gemini, có thể treo cả app | Chưa giao | [ ] |
| 5 | 🔴 | Dừng lớp không gửi thông báo cho ai | B | [ ] |
| 6 | 🔴 | Huỷ được lớp đang học dở | B | [ ] |
| 7 | 🔴 | Lớp dư 1 ngày học | B | [ ] |
| 8 | 🔴 | Tạo được lớp mà không ai đăng ký được | B | [ ] |
| 9 | 🔴 | HLV nào cũng gán được vào môn nào | C | [ ] |
| 10 | 🔴 | Báo cáo doanh thu bỏ sót tiền khi hoàn một phần | B | [ ] |
| 11 | 🔴 | Deploy lên DB production sẽ làm sập trang gói | Cả team | [ ] |
| 12 | 🟠 | Check-in trả "gói đang hiệu lực" lẫn gói chưa dùng được | A | [ ] |
| 13 | 🟠 | Logic tính tiền hoàn chưa có test | A | [ ] |
| 14 | 🟠 | Đăng ký và đổi lớp chưa có test | C | [ ] |
| 15 | 🟠 | Code thừa: 6 mã lỗi cũ, 5 dòng cấu hình | A | [ ] |
| 16 | 🟡 | Dừng được cả lớp chưa bắt đầu | B | [ ] |
| 17 | 🟡 | Lỗi đăng nhập Google để lộ chi tiết bên trong | Chưa giao | [ ] |
| 18 | 🟡 | Bấm "Mua" 2 lần cùng lúc tạo ra 2 đơn chờ | A | [ ] |
| 19 | 🟡 | Giới hạn câu hỏi AI mỗi ngày có thể bị vượt | Chưa giao | [ ] |
| 20 | 🟡 | Gói lớp đã hết hạn vẫn hiện "lớp đang học" | A | [ ] |

**Thứ tự nên làm:** 1 và 4 (vài dòng, rủi ro lớn nhất) → 2, 3 (tiền) → 5–10 (nghiệp vụ) → 11 trước khi deploy → các mục còn lại.

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

### 5. Dừng lớp không gửi thông báo cho ai
- **File:** `service/impl/SportClassServiceImpl.java`, hàm `stopClass` (khoảng dòng 590–600)
- **Lỗi:** Đăng ký được chuyển sang `STOPPED` **trước**, rồi mới gọi `registeredMembers(id)`. Hàm này chỉ đọc đăng ký `REGISTERED`, nên danh sách rỗng và không ai nhận được thông báo.
- **Sửa:** Chuyển dòng `List<Member> members = registeredMembers(id);` lên **trước** vòng lặp đổi trạng thái.
- **Xong khi:** dừng lớp có 5 học viên thì cả 5 người nhận thông báo "Dừng lớp khẩn cấp".

### 6. Huỷ được lớp đang học dở
- **File:** `SportClassServiceImpl.cancelClass`
- **Lỗi:** Hàm chỉ kiểm tra lớp đang `OPENING`. Lớp đã học 2 tuần vẫn huỷ được, gói lớp của học viên bị trả về "chờ kích hoạt", nên họ đăng ký khoá khác và học thêm 1 tháng miễn phí.
- **Sửa:** thêm mã lỗi `CLASS_CANCEL_STARTED` ("Lớp đã bắt đầu, hãy dùng chức năng Dừng lớp!"), rồi ngay sau `requireOpening(sportClass);`:
  ```java
  LocalDateTime first = sportClass.getFirstSessionAt();
  if (first != null && !LocalDateTime.now().isBefore(first)) {
      throw new AppException(ErrorCode.CLASS_CANCEL_STARTED);
  }
  ```
- **Xong khi:** huỷ lớp đã có buổi diễn ra thì nhận 422, và gói lớp của học viên vẫn `ACTIVE`.

### 7. Lớp dư 1 ngày học
- **File:** `SportClassServiceImpl`, khoảng dòng 84 (`createClass`) và 413 (`updateClass`)
- **Lỗi:** `endDate = startDate + durationDays`, và vòng lặp tạo buổi tính **cả** ngày `endDate`. Lớp "30 ngày" thực ra kéo dài 31 ngày, có thể dư 1 buổi.
- **Sửa:** ở cả 2 chỗ:
  ```java
  LocalDate endDate = requestDTO.getStartDate().plusDays(requestDTO.getDurationDays() - 1);
  ```
  Từ đây `endDate` = ngày học cuối cùng, tính luôn ngày đó. Buổi bù (`ClassSessionServiceImpl`) và hạn gói lớp (`SubscriptionServiceImpl`, cộng thêm 1 ngày) đã theo đúng cách hiểu này.
- **Xong khi:** lớp 30 ngày bắt đầu 01/12 có `endDate = 30/12`, và không có buổi học nào sau ngày đó.

### 8. Tạo được lớp mà không ai đăng ký được
- **File:** `SportClassServiceImpl.createClass` và `updateClass`
- **Lỗi:** Code không kiểm tra thời hạn lớp có trùng với gói đang bán hay không. Ví dụ tạo lớp Boxing 45 ngày trong khi chỉ bán gói 30/90 ngày. Phần đăng ký đòi thời hạn gói **bằng** thời hạn lớp, nên lớp luôn trống.
- **Sửa:** inject `MembershipPackageService` và thêm mã lỗi `CLASS_DURATION_NOT_SOLD`. Sau khi kiểm tra `startDate`:
  ```java
  if (!membershipPackageService.getClassDurations(subject.getSubjectId()).contains(requestDTO.getDurationDays())) {
      throw new AppException(ErrorCode.CLASS_DURATION_NOT_SOLD);
  }
  ```
- **Xong khi:** tạo lớp với thời hạn không có gói tương ứng thì nhận 400.

### 9. HLV nào cũng gán được vào môn nào
- **File:** `guard/CoachSubjectGuard.java`, `entity/Coach.java`, `service/impl/CoachServiceImpl.java`
- **Lỗi:** Chuyên môn được kiểm tra bằng cách **so chữ** trong `speciality`, và chữ "fitness" khớp **mọi môn**. HLV mặc định "General Fitness" dạy được Boxing, Bơi, Yoga. Chưa có bảng `coach_subjects` như nghiệp vụ đã chốt.
- **Sửa:** làm theo mục **C1** trong `docs/TODO-ROLE-B-C.md`:
  - thêm `Coach.subjects` (bảng `coach_subjects`);
  - DTO nhận `subjectIds` (bắt buộc ít nhất 1 môn);
  - chặn bỏ môn khi HLV còn lớp của môn đó;
  - thêm API `GET /coaches?subjectId=`;
  - guard chuyển sang so theo id;
  - chạy SQL gán môn cho các HLV đang có.
- **Xong khi:** gán HLV Yoga vào lớp Boxing bị chặn, kể cả với HLV "General Fitness".

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
- **Xong khi:** sau khi deploy, `GET /packages` trên production trả 200.

---

## 🟠 Nên sửa

### 12. Check-in trả "gói đang hiệu lực" lẫn gói chưa dùng được
- **File:** `service/impl/CheckInServiceImpl.java:81`
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

---

## 🟡 Làm thêm khi rảnh

### 16. Dừng được cả lớp chưa bắt đầu
- **File:** `SportClassServiceImpl.stopClass`
- **Lỗi:** Lớp chưa học buổi nào bị "dừng" thay vì "huỷ", nên học viên phải nhờ lễ tân chuyển lớp thay vì tự đăng ký lại.
- **Sửa:** ngược với mục 6. Nếu `firstSessionAt` còn ở tương lai thì báo lỗi, yêu cầu dùng "Huỷ lớp".

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

---

## Đã kiểm tra và ổn
- Quyền của HLV với dữ liệu hội viên: `CoachAccessGuard` chặn đúng ở mọi endpoint.
- JWT kiểm tra phiên bản token và trạng thái tài khoản ở mỗi request.
- API key Gemini gửi qua header, không nằm trong URL. Câu hỏi AI giới hạn 2000 ký tự.
- Múi giờ JVM được đặt về `Asia/Ho_Chi_Minh`.
- Callback VNPay có kiểm tra chữ ký và số tiền.

## Chưa kiểm tra
Cấu trúc bên trong WebSocket, chi tiết `AuditLogAspect`, SupportTicket, toàn bộ FE. Chưa chạy test tải, chưa kiểm tra trên DB production.
