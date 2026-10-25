# Nghiệp vụ gói tập theo buổi & đăng ký lớp (Mô hình 2)

> **Trạng thái:** DRAFT – chờ team chốt
> **Phạm vi:** Gói tập (`MembershipPackage`), đăng ký gói (`Subscription`), đăng ký lớp (`ClassRegistration`), check-in, điểm danh.
> Các mục đánh dấu **[CẦN CHỐT]** là chỗ team phải quyết định trước khi code.

---

## 1. Vấn đề hiện tại

| # | Vấn đề | Nguyên nhân trong code |
|---|---|---|
| 1 | Member có gói bất kỳ là đăng ký được **mọi lớp, mọi môn** | `ClassRegistrationServiceImpl.register` chỉ kiểm tra `existsActiveCovering(memberId, startTime)`, không xét loại gói hay môn |
| 2 | Member có thể đăng ký giữ chỗ rất nhiều lớp mà không tốn gì | Gói chỉ có thời hạn, không giới hạn số buổi |
| 3 | Không có gói "tập tự do" (dùng khu vực, thiết bị, không học lớp) | `MembershipPackage` không có khái niệm loại gói |
| 4 | Mua gói mới khi đang có gói khác thì gói mới bị **nối ra sau** gói cũ, kể cả khi khác môn | `SubscriptionServiceImpl.calculateStartDate` nối theo gói ACTIVE có `endDate` xa nhất của **mọi** gói |

---

## 2. Tổng quan mô hình

Trung tâm có nhiều môn (Gym, Bơi, Yoga, Boxing, Cầu lông...). Hội viên mua **gói** để được sử dụng dịch vụ. Có 2 loại gói:

| | **FREE – Gói tự do** | **CLASS – Gói lớp theo buổi** |
|---|---|---|
| Mục đích | Tự tập: dùng thiết bị, khu vực (phòng máy, hồ bơi...) | Học lớp có lịch, có HLV **+ tự tập tại khu của môn** |
| Gắn môn | Có – chỉ môn `OPEN` (hoặc toàn trung tâm, chỉ gồm khu `OPEN`) | Có – 1 hoặc nhiều môn (`OPEN` hoặc `CLASS_ONLY`) |
| Giới hạn thời gian | Có (`durationDays`) | Có (`durationDays`) |
| Giới hạn số buổi | Không | Có (`sessionLimit`), hoặc không giới hạn |
| Vào khu tự tập | ✅ đến hết `endDate` | ✅ đến hết `endDate`, **kể cả khi đã hết buổi** |
| Check-in tại quầy | ✅ | ✅ (đến học lớp hoặc đến tự tập) |
| Đăng ký lớp | ❌ | ✅ chỉ lớp thuộc môn của gói, khi còn buổi |
| Bị HLV điểm danh | ❌ (không có trong danh sách lớp) | ✅ |

→ Có thể hiểu gọn: **CLASS = FREE + quyền đăng ký lớp**.

**Quyền vào khu theo môn** (`Subject.areaAccess`, chi tiết ở 3.0):

| Môn | `areaAccess` | Bán gói FREE? | Ai được vào khu tự tập |
|---|---|---|---|
| Gym, Bơi, Yoga... | `OPEN` | ✅ | Chủ gói FREE hoặc CLASS của môn |
| Boxing, MMA... (khu đặc thù) | `CLASS_ONLY` | ❌ | Chỉ chủ gói CLASS của môn |

**Ví dụ danh mục gói:**

| Tên gói | Loại | Môn | Số buổi | Hạn dùng |
|---|---|---|---|---|
| Gym tự do 1 tháng | FREE | Gym | – | 30 ngày |
| Bơi tự do 3 tháng | FREE | Bơi | – | 90 ngày |
| Yoga 12 buổi | CLASS | Yoga | 12 | 45 ngày |
| Boxing 45 buổi | CLASS | Boxing | 45 | 180 ngày |
| Combo Yoga + Pilates 20 buổi | CLASS | Yoga, Pilates | 20 | 90 ngày |
| Yoga không giới hạn 1 tháng | CLASS | Yoga | không giới hạn | 30 ngày |

### Khái niệm "buổi" trong hệ thống

Thiết kế hiện tại: **1 `SportClass` = 1 buổi học** (một cặp `startTime`/`endTime`, 1 phòng, 1 HLV). `Subject` chỉ là danh mục môn.

```
Subject (Boxing)
  └── SportClass  Boxing 18:00–19:30 25/10   ← 1 buổi
  └── SportClass  Boxing 18:00–19:30 27/10   ← 1 buổi
        ├── ClassRegistration  (member đăng ký buổi này → trừ 1 buổi của gói)
        └── ClassAttendance    (HLV điểm danh buổi này)
```

→ Mỗi lần đăng ký 1 `SportClass` = dùng 1 buổi của gói. **Không cần đổi cấu trúc lớp.**

---

## 3. Business Rules

### 3.0. Quyền vào khu tập theo môn

| Mã | Rule |
|---|---|
| **BR-SUBJ-01** | Mỗi `Subject` có `areaAccess` ∈ {`OPEN`, `CLASS_ONLY`}. `OPEN`: khu chung (Gym, Bơi...), chủ gói FREE hoặc CLASS của môn đều vào được. `CLASS_ONLY`: khu đặc thù (Boxing, MMA...) cần HLV hướng dẫn, chỉ chủ gói CLASS của môn mới được học và tập. |
| **BR-SUBJ-02** | Không được tạo gói FREE áp dụng cho môn `CLASS_ONLY`. |
| **BR-SUBJ-03** | Đổi môn `OPEN` → `CLASS_ONLY` khi còn gói FREE `ACTIVE` của môn đó: **[CẦN CHỐT]** chặn hay tự chuyển các gói đó sang `INACTIVE`. Đề xuất: **chặn**, Manager tự chuyển gói sang `INACTIVE` trước. Subscription đã mua vẫn dùng đến hết hạn (giống BR-PKG-07). Đổi `CLASS_ONLY` → `OPEN` không cần xử lý gì. |
| **BR-SUBJ-04** | Gói CLASS áp dụng nhiều môn (combo) → member được vào khu tự tập của **tất cả** các môn trong gói. |

### 3.1. Gói tập (Manager quản lý)

| Mã | Rule |
|---|---|
| **BR-PKG-01** | Mỗi gói có loại `packageType` ∈ {`FREE`, `CLASS`}. |
| **BR-PKG-02** | Gói **CLASS** phải áp dụng cho ít nhất 1 môn đang `ACTIVE`. |
| **BR-PKG-03** | Gói **FREE** áp dụng cho 1 môn `OPEN` (khu tự tập của môn đó), hoặc "toàn trung tâm" (không gắn môn). Gói toàn trung tâm **chỉ vào được các khu `OPEN`**, không bao gồm khu `CLASS_ONLY`. |
| **BR-PKG-04** | Mọi gói đều **bắt buộc** có `durationDays > 0`. Không có gói vô thời hạn. |
| **BR-PKG-05** | Gói CLASS có `sessionLimit`: số nguyên > 0, hoặc `null` = không giới hạn số buổi. Gói FREE luôn `sessionLimit = null`. |
| **BR-PKG-06** | Gói đã có người mua (có `Subscription`) thì **không được sửa** `packageType`, danh sách môn, `sessionLimit`, `durationDays`. Chỉ được sửa tên, mô tả, giá, trạng thái. Giá mới chỉ áp dụng cho lượt mua sau. |
| **BR-PKG-07** | Gói `INACTIVE` không bán được nữa, nhưng subscription đã mua vẫn dùng bình thường đến hết hạn. |
| **BR-PKG-08** | Không cho xoá mềm môn nếu còn gói `ACTIVE` áp dụng cho môn đó. **[CẦN CHỐT]** chặn hay tự chuyển các gói đó sang `INACTIVE`. |

### 3.2. Mua gói & thời hạn (Subscription)

| Mã | Rule |
|---|---|
| **BR-SUB-01** | Gói chỉ `ACTIVE` sau khi thanh toán thành công (giữ nguyên luồng hiện tại: lễ tân xác nhận / VNPay callback). |
| **BR-SUB-02** | Member được sở hữu **nhiều gói cùng lúc**, khác loại hoặc khác môn (VD: Gym tự do + Yoga 12 buổi). |
| **BR-SUB-03** | Qua `endDate` → gói hết hiệu lực hoàn toàn. Gói CLASS dùng hết `sessionLimit` buổi → **chỉ mất quyền đăng ký lớp**, vẫn vào khu tự tập của môn đến hết `endDate`. |
| **BR-SUB-04** | Hết hạn mà còn buổi → **số buổi còn lại bị mất**. |
| **BR-SUB-05** | **Nối hạn chỉ trong cùng phạm vi:** khi mua gói mới, nếu member đang có gói ACTIVE **cùng loại và cùng tập môn** còn hạn **và còn buổi**, thì gói mới bắt đầu từ `endDate` của gói đó. Ngược lại gói mới bắt đầu **ngay khi thanh toán**. |
| **BR-SUB-06** | Mỗi member chỉ có tối đa 1 giao dịch đang chờ thanh toán (giữ nguyên rule hiện tại). |
| **BR-SUB-07** | Nhắc gói sắp hết hạn: giữ nguyên theo ngày (`app.reminder.subscription-days-before`). **[CẦN CHỐT]** có nhắc thêm khi gói CLASS còn ≤ 2 buổi không. |
| **BR-SUB-08** | **Cảnh báo mua gói thừa trước khi thanh toán.** Gói mới bị coi là thừa khi member đang có gói `ACTIVE` (hoặc đã thanh toán, chưa bắt đầu) **đã cấp quyền vào cùng khu tự tập** và thời gian trùng với gói mới. Khi đó phải hiển thị cho member: tên gói đang có, loại, ngày hết hạn, số ngày còn lại, số buổi còn lại (nếu là CLASS). **Chỉ cảnh báo, không chặn**: member xác nhận thì vẫn mua được. Lễ tân bán tại quầy cũng thấy cảnh báo này. |

**Các trường hợp cảnh báo (BR-SUB-08):**

| Gói đang có | Gói định mua | Cảnh báo? |
|---|---|---|
| CLASS Gym | FREE Gym | ✅ "Gói CLASS Gym đã bao gồm quyền tự tập đến dd/mm (còn X ngày)" |
| FREE Gym | CLASS Gym | ✅ "Bạn đang có FREE Gym đến dd/mm (còn X ngày). Gói CLASS mới đã bao gồm quyền tự tập" |
| FREE toàn trung tâm | FREE Gym | ✅ "Gói toàn trung tâm đã bao gồm khu Gym đến dd/mm" |
| FREE Gym | FREE Gym | ❌ BR-SUB-05 nối hạn, không trùng thời gian |
| CLASS Gym còn buổi | CLASS Gym | ❌ BR-SUB-05 nối hạn |
| FREE Gym | CLASS Boxing | ❌ Khác khu |

### 3.3. Đăng ký lớp

| Mã | Rule |
|---|---|
| **BR-REG-01** | Chỉ đăng ký được lớp đang `OPENING` và **chưa bắt đầu** (giữ nguyên). |
| **BR-REG-02** | Member phải có ít nhất 1 gói **hợp lệ** cho lớp. Gói hợp lệ khi thoả **tất cả**: `status = ACTIVE`; `packageType = CLASS`; gói áp dụng cho **môn của lớp**; `startDate <= class.startTime < endDate`; còn buổi (`sessionLimit = null` hoặc `usedSessions < sessionLimit`). |
| **BR-REG-03** | Gói FREE **không bao giờ** dùng để đăng ký lớp. |
| **BR-REG-04** | Nếu có nhiều gói hợp lệ, hệ thống tự chọn gói có **`endDate` sớm nhất** để trừ buổi. |
| **BR-REG-05** | Đăng ký thành công → **trừ 1 buổi** của gói được chọn ngay lúc đăng ký và ghi lại đăng ký đó trừ vào gói nào (`ClassRegistration.subscription`). |
| **BR-REG-06** | Không đăng ký trùng lớp, không trùng giờ với lớp khác đã đăng ký, lớp còn chỗ (giữ nguyên). |
| **BR-REG-07** | Lễ tân đăng ký hộ dùng chung toàn bộ rule trên (`ReceptionistRegistrationController` đã gọi chung `ClassRegistrationService`). |
| **BR-REG-08** | Các thao tác kiểm tra + trừ buổi nằm trong **1 transaction**, có khoá dòng member và lớp (đã có `findByIdForUpdate`) để tránh 2 request cùng trừ buổi cuối. |

**Thứ tự báo lỗi khi không có gói hợp lệ** (để FE hiển thị đúng lý do):

| Thứ tự | Điều kiện | Mã lỗi |
|---|---|---|
| 1 | Không có gói nào còn hạn vào giờ học | `NO_ACTIVE_SUBSCRIPTION` / `SUBSCRIPTION_EXPIRED` (đã có) |
| 2 | Chỉ có gói FREE | `PACKAGE_NOT_ALLOW_CLASS` (mới) |
| 3 | Có gói CLASS nhưng không áp dụng cho môn này | `SUBJECT_NOT_COVERED` (mới) |
| 4 | Có gói CLASS đúng môn nhưng đã hết buổi | `NO_SESSIONS_LEFT` (mới) |

### 3.4. Huỷ đăng ký & hoàn buổi

| Mã | Rule |
|---|---|
| **BR-CAN-01** | Member huỷ **trước** giờ học ít nhất `app.booking.cancel-cutoff-hours` (hiện tại 2 giờ) → **hoàn 1 buổi** vào đúng gói đã trừ. |
| **BR-CAN-02** | Quá hạn huỷ → không cho huỷ (`CANCEL_NOT_ALLOWED`, giữ nguyên). Buổi đó coi như đã dùng. |
| **BR-CAN-03** | Huỷ một đăng ký đã `CANCELLED` → trả về trạng thái hiện tại, **không hoàn lần 2** (giữ nguyên tính idempotent). |
| **BR-CAN-04** | Manager **huỷ lớp** (`cancelClass`) hoặc **xoá lớp** (`deleteClass`) → hoàn 1 buổi cho **mọi** member đang đăng ký, không xét cutoff (lỗi do trung tâm). |
| **BR-CAN-05** | Hoàn buổi vào gói đã hết `endDate` vẫn ghi nhận (để số liệu đúng) nhưng member không dùng được nữa. **[CẦN CHỐT]** có cộng thêm ngày cho gói trong trường hợp lớp bị huỷ sát ngày hết hạn không. |

### 3.5. Điểm danh

| Mã | Rule |
|---|---|
| **BR-ATT-01** | HLV chỉ điểm danh member đã đăng ký lớp (giữ nguyên, `MEMBER_NOT_IN_CLASS`). Member gói FREE tự nhiên không có trong danh sách. |
| **BR-ATT-02** | Điểm danh **không** trừ hay hoàn buổi. Buổi đã trừ lúc đăng ký → vắng mặt (ABSENT) thì mất buổi. |

### 3.6. Check-in tại quầy

| Mã | Rule |
|---|---|
| **BR-CHK-01** | Member có **ít nhất 1 gói ACTIVE còn hạn** (FREE hoặc CLASS) thì được check-in. Mỗi ngày 1 lần (giữ nguyên). |
| **BR-CHK-02** | Response check-in trả về **danh sách gói đang hiệu lực** (loại, môn, `areaAccess` của môn, số buổi còn lại, ngày hết hạn) để lễ tân biết member được vào khu nào. Gói CLASS hết buổi nhưng còn hạn vẫn nằm trong danh sách (BR-SUB-03). |
| **BR-CHK-03** | **[CẦN CHỐT]** có check-in theo môn không (lễ tân chọn môn, `CheckIn` thêm `subject_id`, rule thành 1 lần/ngày/môn). Đề xuất: **chưa làm**, giữ check-in chung. Khi đó khu `CLASS_ONLY` được kiểm soát bằng việc không bán gói FREE (BR-SUBJ-02) và lễ tân xem `areaAccess` trong response check-in. |
| **BR-CHK-04** | Check-in **không** trừ buổi. |

---

## 4. Luồng nghiệp vụ

### 4.1. Đăng ký lớp

```
Member chọn lớp Boxing 18:00 27/10
        │
        ▼
Khoá member + khoá lớp
        │
Lớp OPENING & chưa bắt đầu? ──no──► CLASS_NOT_OPEN
        │yes
Tìm gói hợp lệ (BR-REG-02) ──none──► báo lỗi theo bảng thứ tự 3.3
        │found → chọn gói endDate sớm nhất
Đã đăng ký lớp này? ──yes──► ALREADY_REGISTERED
Trùng giờ lớp khác? ──yes──► MEMBER_SCHEDULE_CONFLICT
Lớp đầy? ──yes──► CLASS_FULL
        │
        ▼
Lưu ClassRegistration(status=REGISTERED, subscription=gói đã chọn)
gói.usedSessions += 1
```

### 4.2. Huỷ đăng ký

```
Member huỷ đăng ký
        │
Đã CANCELLED? ──yes──► trả về, không làm gì
Quá cutoff? ──yes──► CANCEL_NOT_ALLOWED
        │
status = CANCELLED, cancelledAt = now
registration.subscription.usedSessions -= 1
```

### 4.3. Mua gói (cảnh báo gói thừa)

```
Member chọn gói, bấm "Mua"
        │
        ▼
FE gọi purchase-preview (packageId)
        │
BE tính startDate / endDate dự kiến (BR-SUB-05)
BE tìm gói đang có cấp quyền vào cùng khu, trùng thời gian (BR-SUB-08)
        │
Có cảnh báo? ──no──► sang bước thanh toán
        │yes
        ▼
FE hiện hộp xác nhận: tên gói đang có, hạn đến dd/mm, còn X ngày, còn Y buổi
        │
Member hủy ──► dừng │ Member xác nhận ──► sang bước thanh toán
```

Đề xuất thêm (tuỳ chọn): request mua gói gửi `acknowledgedWarnings = true`, BE từ chối nếu có cảnh báo mà cờ này chưa bật, để chắc chắn FE đã hiển thị cảnh báo.

### 4.4. Ví dụ end-to-end

> An mua **Boxing 45 buổi / 180 ngày** ngày 01/11 và **Gym tự do 1 tháng** ngày 05/11.

| Ngày | Sự kiện | Boxing (đã dùng/45) | Ghi chú |
|---|---|---|---|
| 01/11 | Thanh toán gói Boxing | 0 | Hạn đến 30/04 |
| 05/11 | Thanh toán gói Gym | 0 | Gym bắt đầu **ngay** 05/11 (khác loại → không nối) |
| 06/11 | Đăng ký lớp Boxing 08/11 | 1 | |
| 06/11 | Đăng ký lớp Yoga 09/11 | 1 | ❌ `SUBJECT_NOT_COVERED` |
| 07/11 | Huỷ lớp 08/11 (trước 2h) | 0 | Hoàn buổi |
| 08/11 | Check-in quầy, tập gym | 0 | Hợp lệ nhờ gói Gym |
| 10/11 | Đăng ký lớp Boxing 12/11, không đến | 1 | Điểm danh ABSENT → mất buổi |
| 15/11 | Trung tâm huỷ lớp Boxing 16/11 mà An đã đăng ký | 1 | Hoàn buổi (đã đăng ký 16/11 → 2, huỷ → 1) |
| 30/04 | Gói Boxing hết hạn khi mới dùng 40 buổi | 40 | 5 buổi còn lại bị mất |

> Nếu An dùng hết 45 buổi Boxing trước 30/04 thì từ lúc đó không đăng ký lớp Boxing được nữa (`NO_SESSIONS_LEFT`), nhưng vẫn vào khu Boxing tự tập đến 30/04 (BR-SUB-03).

---

## 5. Thay đổi dữ liệu

| Bảng | Thay đổi | Ghi chú |
|---|---|---|
| `subjects` | + `area_access` (`OPEN`/`CLASS_ONLY`, NOT NULL, default `OPEN`) | BR-SUBJ-01 |
| `packages` | + `package_type` (`FREE`/`CLASS`, NOT NULL) | |
| `packages` | + `session_limit` (INT, NULL) | `null` = không giới hạn |
| `package_subjects` (mới) | `package_id`, `subject_id` | Quan hệ nhiều–nhiều gói ↔ môn |
| `subscriptions` | + `used_sessions` (INT, default 0) | |
| `class_registrations` | + `subscription_id` (FK, NULL cho dữ liệu cũ) | Biết đăng ký trừ vào gói nào để hoàn |

**Dữ liệu cũ [CẦN CHỐT]:** các gói hiện có chuyển thành loại gì? Nếu mặc định `FREE` thì member cũ mất quyền đăng ký lớp. Đề xuất: reset lại dữ liệu test (DataInitializer / Postman setup) theo danh mục mới.

---

## 6. Ảnh hưởng tới code hiện tại

| Chỗ | Thay đổi |
|---|---|
| `Subject` + DTO + `SubjectServiceImpl` | Thêm `areaAccess`, kiểm tra BR-SUBJ-03 khi đổi `OPEN` → `CLASS_ONLY` |
| `MembershipPackage` + DTO + `MembershipPackageServiceImp` | Thêm trường, validate BR-PKG-01…08 và BR-SUBJ-02 |
| `SubscriptionRepository` | Query tìm gói hợp lệ cho lớp (BR-REG-02), sắp theo `endDate`; query tìm gói trùng khu (BR-SUB-08) |
| `SubscriptionServiceImpl.calculateStartDate` | Nối hạn theo phạm vi (BR-SUB-05) |
| `SubscriptionService` + controller | API mới `purchase-preview`: trả `startDate`/`endDate` dự kiến và danh sách cảnh báo (BR-SUB-08) |
| `SubscriptionServiceImpl.getCurrentSubscription` | Trả **danh sách** gói đang hiệu lực, kèm số buổi còn lại |
| `ClassRegistrationServiceImpl.register` | Thay bước kiểm tra gói, trừ buổi |
| `ClassRegistrationServiceImpl.cancel` | Hoàn buổi |
| `SportClassServiceImpl.cancelClass` / `deleteClass` | Hoàn buổi trong vòng lặp huỷ đăng ký |
| `CheckInServiceImpl` | Trả danh sách gói hiệu lực kèm `areaAccess` (BR-CHK-02) |
| `ErrorCode` | + `PACKAGE_NOT_ALLOW_CLASS`, `SUBJECT_NOT_COVERED`, `NO_SESSIONS_LEFT`, `FREE_PACKAGE_NOT_ALLOWED` (BR-SUBJ-02), `SUBJECT_HAS_ACTIVE_FREE_PACKAGE` (BR-SUBJ-03) |
| `CoachServiceImpl.takeAttendance` | **Không đổi** |

---

## 7. Test case chính (Postman)

| # | Kịch bản | Kết quả mong đợi |
|---|---|---|
| T1 | Member chỉ có gói FREE đăng ký lớp | 422 `PACKAGE_NOT_ALLOW_CLASS` |
| T2 | Gói Yoga đăng ký lớp Boxing | 422 `SUBJECT_NOT_COVERED` |
| T3 | Gói Yoga 1 buổi, đăng ký lớp Yoga thứ 2 | 422 `NO_SESSIONS_LEFT` |
| T4 | Gói Yoga 12 buổi đăng ký lớp Yoga | 201, `usedSessions = 1` |
| T5 | Huỷ trước cutoff | 200, `usedSessions` giảm 1 |
| T6 | Huỷ 2 lần | Lần 2 trả về CANCELLED, `usedSessions` không đổi |
| T7 | Manager huỷ lớp có 3 người đăng ký | Cả 3 được hoàn buổi |
| T8 | Có 2 gói Yoga (hết hạn 10/11 và 30/11) | Trừ vào gói hết hạn 10/11 |
| T9 | Đang có Gym FREE, mua Yoga CLASS | Yoga bắt đầu ngay, không nối sau Gym |
| T10 | Đang có Yoga 12 buổi còn hạn, mua thêm Yoga 12 buổi | Gói mới nối sau `endDate` gói cũ |
| T11 | Gói còn ngày nhưng hết buổi, mua gói mới cùng môn | Gói mới bắt đầu ngay |
| T12 | Member gói FREE check-in | 201, response có danh sách gói |
| T13 | 2 request đăng ký cùng lúc khi gói còn 1 buổi | 1 thành công, 1 `NO_SESSIONS_LEFT` |
| T14 | Manager tạo gói FREE cho môn Boxing (`CLASS_ONLY`) | 400 `FREE_PACKAGE_NOT_ALLOWED` |
| T15 | Đổi môn Gym sang `CLASS_ONLY` khi còn gói FREE Gym `ACTIVE` | 409 `SUBJECT_HAS_ACTIVE_FREE_PACKAGE` (nếu chốt chặn) |
| T16 | Gói CLASS Boxing hết buổi nhưng còn hạn, member check-in | 201, response vẫn có gói Boxing, số buổi còn lại = 0 |
| T17 | Đang có CLASS Gym, gọi preview mua FREE Gym | 200, có 1 cảnh báo kèm ngày hết hạn và số ngày còn lại |
| T18 | Đang có FREE Gym, gọi preview mua FREE Gym | 200, không cảnh báo, `startDate` = `endDate` gói cũ |
| T19 | Đang có FREE Gym, gọi preview mua CLASS Boxing | 200, không cảnh báo |

---

## 8. Ngoài phạm vi (làm sau nếu còn thời gian)

- Bảo lưu gói (tạm dừng, cộng thêm ngày).
- Hạn gói tính từ buổi học đầu tiên thay vì ngày thanh toán.
- Danh sách chờ khi lớp đầy.
- Phạt huỷ muộn / không đến (khoá quyền đặt lớp vài ngày).
- Lớp tốn nhiều hơn 1 buổi (workshop).
- Check-in theo môn / khu vực (hiện khu `CLASS_ONLY` kiểm soát qua BR-SUBJ-02 và lễ tân, xem BR-CHK-03).
- Buổi học thử cho khách chưa mua gói.
- Chuyển nhượng gói cho người khác.
- Tạo lớp lặp lại theo lịch tuần (sinh nhiều `SportClass` một lần).
- Khoá học cố định (`Course` nhóm nhiều buổi, đăng ký cả khoá).

---

## 9. Checklist để team chốt

- [x] Đồng ý 2 loại gói FREE / CLASS (CLASS = FREE + đăng ký lớp)
- [x] BR-PKG-03: có gói FREE "toàn trung tâm", chỉ gồm khu `OPEN`
- [x] BR-SUBJ-01: `areaAccess` chỉ 2 giá trị `OPEN` / `CLASS_ONLY` (bỏ `NONE`)
- [x] BR-SUB-03: hết buổi nhưng còn hạn vẫn vào khu tự tập
- [x] BR-SUB-08: mua gói thừa → cảnh báo trước khi thanh toán, không chặn
- [ ] BR-SUBJ-03: đổi môn sang `CLASS_ONLY` khi còn gói FREE → chặn hay tự INACTIVE (đề xuất: chặn)
- [ ] BR-SUB-08: có bắt buộc `acknowledgedWarnings` khi mua không
- [ ] BR-PKG-08: xoá môn còn gói → chặn hay tự INACTIVE gói
- [ ] BR-SUB-07: có nhắc khi gói sắp hết buổi không
- [ ] BR-CAN-05: lớp bị huỷ sát hạn gói có cộng ngày không
- [ ] BR-CHK-03: check-in chung hay theo môn
- [ ] Cutoff huỷ lớp giữ 2 giờ hay đổi
- [ ] Cách xử lý dữ liệu gói cũ (mục 5)
- [ ] Chia giai đoạn: (1) FREE/CLASS + đúng môn + nối hạn → (2) số buổi, trừ/hoàn buổi
