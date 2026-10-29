# Nghiệp vụ gói tập theo buổi & đăng ký lớp (Mô hình 2)

> **Trạng thái:** ĐÃ CODE (commit `d29d098`, 26/10/2026). Tài liệu này mô tả đúng hành vi code hiện tại trên branch `hotfix` (cập nhật 28/10/2026).
> **Phạm vi:** Gói tập (`MembershipPackage`), đăng ký gói (`Subscription`), đăng ký lớp (`ClassRegistration`), hoàn tiền, check-in, điểm danh.
> Các điểm team còn đang xem xét nằm ở **mục 9**. Code hiện tại vẫn chạy theo rule ghi trong mục 3 cho tới khi team chốt thay đổi.

---

## 1. Vấn đề trước đây (đã giải quyết)

| # | Vấn đề | Cách giải quyết |
|---|---|---|
| 1 | Member có gói bất kỳ là đăng ký được **mọi lớp, mọi môn** | Chỉ gói CLASS **đúng môn** của lớp mới đăng ký được (BR-REG-02) |
| 2 | Member có thể đăng ký giữ chỗ rất nhiều lớp mà không tốn gì | Mỗi lần đăng ký trừ 1 buổi của gói (BR-REG-05). Riêng gói CLASS không giới hạn buổi vẫn còn vấn đề này, xem mục 9 |
| 3 | Không có gói "tập tự do" (dùng khu vực, thiết bị, không học lớp) | Thêm loại gói `FREE` (BR-PKG-01) |
| 4 | Mua gói mới khi đang có gói khác thì gói mới bị **nối ra sau** gói cũ, kể cả khi khác môn | Chỉ nối hạn khi cùng loại và cùng tập môn (BR-SUB-05) |

---

## 2. Tổng quan mô hình

Trung tâm có nhiều môn (Gym, Bơi, Yoga, Boxing, Cầu lông...). Hội viên mua **gói** để được sử dụng dịch vụ. Có 2 loại gói:

| | **FREE – Gói tự do** | **CLASS – Gói lớp theo buổi** |
|---|---|---|
| Mục đích | Tự tập: dùng thiết bị, khu vực (phòng máy, hồ bơi...) | Học lớp có lịch, có HLV **+ tự tập tại khu của môn** |
| Gắn môn | Tối đa 1 môn `OPEN`, hoặc không gắn môn = toàn trung tâm (chỉ gồm khu `OPEN`) | Ít nhất 1 môn, được nhiều môn (combo), `OPEN` hoặc `CLASS_ONLY` |
| Giới hạn thời gian | Có (`durationDays`) | Có (`durationDays`) |
| Giới hạn số buổi | Không | Có (`sessionLimit`), hoặc `null` = không giới hạn |
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

**1 `SportClass` = 1 buổi học** (một cặp `startTime`/`endTime`, 1 phòng, 1 HLV). `Subject` chỉ là danh mục môn.

```
Subject (Boxing)
  └── SportClass  Boxing 18:00–19:30 25/10   ← 1 buổi
  └── SportClass  Boxing 18:00–19:30 27/10   ← 1 buổi
        ├── ClassRegistration  (member đăng ký buổi này → trừ 1 buổi của gói)
        └── ClassAttendance    (HLV điểm danh buổi này)
```

→ Mỗi lần đăng ký 1 `SportClass` = dùng 1 buổi của gói.

---

## 3. Business Rules

### 3.0. Quyền vào khu tập theo môn

| Mã | Rule |
|---|---|
| **BR-SUBJ-01** | Mỗi `Subject` có `areaAccess` ∈ {`OPEN`, `CLASS_ONLY`}, mặc định `OPEN` khi tạo môn. `OPEN`: khu chung (Gym, Bơi...), chủ gói FREE hoặc CLASS của môn đều vào được. `CLASS_ONLY`: khu đặc thù (Boxing, MMA...) cần HLV hướng dẫn, chỉ chủ gói CLASS của môn mới được học và tập. |
| **BR-SUBJ-02** | Không được tạo gói FREE áp dụng cho môn `CLASS_ONLY` → 400 `FREE_PACKAGE_NOT_ALLOWED`. |
| **BR-SUBJ-03** | Đổi môn `OPEN` → `CLASS_ONLY` khi còn gói FREE `ACTIVE` (đang bán) của môn đó → **chặn**, 409 `SUBJECT_HAS_ACTIVE_FREE_PACKAGE`. Manager tự chuyển các gói đó sang `INACTIVE` trước. Subscription đã mua vẫn dùng đến hết hạn (giống BR-PKG-07). Đổi `CLASS_ONLY` → `OPEN` không cần xử lý gì. |
| **BR-SUBJ-04** | Gói CLASS áp dụng nhiều môn (combo) → member được vào khu tự tập của **tất cả** các môn trong gói. |

### 3.1. Gói tập (Manager quản lý)

| Mã | Rule |
|---|---|
| **BR-PKG-01** | Mỗi gói có loại `packageType` ∈ {`FREE`, `CLASS`}. |
| **BR-PKG-02** | Gói **CLASS** phải áp dụng cho ít nhất 1 môn → nếu không: 400 `PACKAGE_SUBJECT_REQUIRED`. |
| **BR-PKG-03** | Gói **FREE** áp dụng cho tối đa 1 môn `OPEN`, hoặc không gắn môn = "toàn trung tâm". Gói toàn trung tâm **chỉ vào được các khu `OPEN`**. Gắn nhiều hơn 1 môn hoặc có `sessionLimit` → 400 `INVALID_PACKAGE_CONFIG`. |
| **BR-PKG-04** | Mọi gói đều **bắt buộc** có `durationDays > 0`. Không có gói vô thời hạn. |
| **BR-PKG-05** | Gói CLASS có `sessionLimit`: số nguyên > 0, hoặc `null` = không giới hạn số buổi. Gói FREE luôn `sessionLimit = null`. |
| **BR-PKG-06** | Gói đã có người mua (có `Subscription`) thì **không được sửa** `packageType`, danh sách môn, `sessionLimit`, `durationDays` → 409 `PACKAGE_IN_USE`. Chỉ được sửa tên, mô tả, giá, trạng thái. Giá mới chỉ áp dụng cho lượt mua sau. |
| **BR-PKG-07** | Xoá gói = chuyển `INACTIVE` (không xoá dòng). Gói `INACTIVE` không bán được nữa, nhưng subscription đã mua vẫn dùng bình thường đến hết hạn. Manager mở bán lại được. |
| **BR-PKG-08** | Xoá mềm môn khi còn gói `ACTIVE` (đang bán) áp dụng cho môn đó → **chặn**, 409 `SUBJECT_HAS_ACTIVE_PACKAGE`. Manager tự chuyển các gói đó sang `INACTIVE` trước. |

### 3.2. Mua gói & thời hạn (Subscription)

| Mã | Rule |
|---|---|
| **BR-SUB-01** | Gói chỉ `ACTIVE` sau khi thanh toán thành công (lễ tân xác nhận đã thu tiền / VNPay callback). |
| **BR-SUB-02** | Member được sở hữu **nhiều gói cùng lúc**, khác loại hoặc khác môn (VD: Gym tự do + Yoga 12 buổi). |
| **BR-SUB-03** | Qua `endDate` → gói hết hiệu lực hoàn toàn. Gói CLASS dùng hết `sessionLimit` buổi → **chỉ mất quyền đăng ký lớp**, vẫn vào khu tự tập của môn đến hết `endDate`. |
| **BR-SUB-04** | Hết hạn mà còn buổi → **số buổi còn lại bị mất**. |
| **BR-SUB-05** | **Nối hạn chỉ trong cùng phạm vi:** khi mua gói mới, nếu member đang có gói ACTIVE **cùng loại và cùng tập môn**, còn hạn **và còn buổi**, thì gói mới bắt đầu từ `endDate` xa nhất của các gói đó. Ngược lại gói mới bắt đầu **ngay khi thanh toán**. Xem thêm điểm 9.1. |
| **BR-SUB-06** | Mỗi member chỉ có tối đa 1 giao dịch đang chờ thanh toán. |
| **BR-SUB-07** | Nhắc **từng gói** sắp hết hạn theo ngày (`app.reminder.subscription-days-before`), mỗi gói nhắc 1 lần. Chỉ **bỏ qua** khi member đã có gói `ACTIVE` **cùng loại và cùng tập môn** hết hạn sau gói đó (đã gia hạn, xem BR-SUB-05). Gói khác loại hoặc khác môn hết hạn muộn hơn **không** làm mất lời nhắc. **Không** nhắc khi gói CLASS sắp hết buổi. |
| **BR-SUB-08** | **Cảnh báo mua gói thừa trước khi thanh toán.** Gói mới bị coi là thừa khi member đang có gói `ACTIVE` (hoặc đã thanh toán, chưa bắt đầu) **đã cấp quyền vào cùng khu tự tập** và thời gian trùng với gói mới. Cảnh báo gồm: tên gói đang có, loại, ngày hết hạn, số ngày còn lại, số buổi còn lại (nếu là CLASS). **Chỉ cảnh báo, không chặn**: request mua gói **không** cần gửi cờ xác nhận (`acknowledgedWarnings`), FE tự hiện hộp xác nhận. Member xem qua `GET /api/v1/members/me/subscriptions/preview?packageId=`, lễ tân xem qua `GET /api/v1/receptionist/members/{memberId}/subscriptions/preview?packageId=`. |

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
| **BR-REG-01** | Chỉ đăng ký được lớp đang `OPENING` và **chưa bắt đầu** → nếu không: `CLASS_NOT_OPEN`. |
| **BR-REG-02** | Member phải có ít nhất 1 gói **hợp lệ** cho lớp. Gói hợp lệ khi thoả **tất cả**: `status = ACTIVE`; `packageType = CLASS`; gói áp dụng cho **môn của lớp**; `startDate <= class.startTime < endDate`; còn buổi (`sessionLimit = null` hoặc `usedSessions < sessionLimit`). |
| **BR-REG-03** | Gói FREE **không bao giờ** dùng để đăng ký lớp. |
| **BR-REG-04** | Nếu có nhiều gói hợp lệ, hệ thống tự chọn gói có **`endDate` sớm nhất** để trừ buổi. |
| **BR-REG-05** | Đăng ký thành công → **trừ 1 buổi** của gói được chọn ngay lúc đăng ký và ghi lại đăng ký đó trừ vào gói nào (`ClassRegistration.subscription`). Đăng ký lại một lớp đã huỷ trước đó thì dùng lại dòng đăng ký cũ và trừ buổi lại. |
| **BR-REG-06** | Không đăng ký trùng lớp (`ALREADY_REGISTERED`), không trùng giờ với lớp khác đã đăng ký (`MEMBER_SCHEDULE_CONFLICT`), lớp còn chỗ (`CLASS_FULL`). |
| **BR-REG-07** | Lễ tân đăng ký hộ dùng chung toàn bộ rule về gói ở trên. |
| **BR-REG-08** | Các thao tác kiểm tra + trừ buổi nằm trong **1 transaction**, khoá dòng member, lớp và gói (`findByIdForUpdate`) để tránh 2 request cùng trừ buổi cuối. |
| **BR-REG-09** | Member **tự** đăng ký lớp thì phải hoàn thành bài khảo sát (quiz) trước → nếu chưa: 422 `QUIZ_NOT_COMPLETED`. Lễ tân đăng ký hộ **không** bị chặn bởi quiz. |

**Thứ tự báo lỗi khi không có gói hợp lệ** (để FE hiển thị đúng lý do):

| Thứ tự | Điều kiện | Mã lỗi |
|---|---|---|
| 1 | Không có gói nào còn hạn vào giờ học | `NO_ACTIVE_SUBSCRIPTION` / `SUBSCRIPTION_EXPIRED` |
| 2 | Chỉ có gói FREE | `PACKAGE_NOT_ALLOW_CLASS` |
| 3 | Có gói CLASS nhưng không áp dụng cho môn này | `SUBJECT_NOT_COVERED` |
| 4 | Có gói CLASS đúng môn nhưng đã hết buổi | `NO_SESSIONS_LEFT` |

### 3.4. Huỷ đăng ký & hoàn buổi

| Mã | Rule |
|---|---|
| **BR-CAN-01** | Member huỷ **trước** giờ học ít nhất `app.booking.cancel-cutoff-hours` (**2 giờ**) → **hoàn 1 buổi** vào đúng gói đã trừ. |
| **BR-CAN-02** | Quá hạn huỷ → không cho huỷ (`CANCEL_NOT_ALLOWED`). Buổi đó coi như đã dùng. |
| **BR-CAN-03** | Huỷ một đăng ký đã `CANCELLED` → trả về trạng thái hiện tại, **không hoàn lần 2**. |
| **BR-CAN-04** | Manager **huỷ lớp** (`cancelClass`) hoặc **xoá lớp** (`deleteClass`) → hoàn 1 buổi cho **mọi** member đang đăng ký, không xét cutoff (lỗi do trung tâm). |
| **BR-CAN-05** | Hoàn buổi vào gói đã hết `endDate` vẫn ghi nhận (để số liệu đúng) nhưng member không dùng được nữa. **Không** cộng thêm ngày cho gói khi lớp bị huỷ sát ngày hết hạn. |
| **BR-CAN-06** | Đăng ký cũ không gắn gói (tạo trước khi có mô hình này, `subscription = null`) thì huỷ không hoàn buổi. |
| **BR-CAN-07** | Manager **khoá tài khoản member** → mọi đăng ký lớp **sắp diễn ra** của member bị huỷ (`CANCELLED`) để trả chỗ cho lớp, nhưng **không hoàn buổi**: các buổi đã trừ bị mất. Mở khoá lại không khôi phục đăng ký hay buổi đã mất. |

### 3.5. Hoàn tiền gói

| Mã | Rule |
|---|---|
| **BR-PAY-01** | Manager hoàn tiền giao dịch đã thu → giao dịch `REFUNDED`, gói của giao dịch chuyển `CANCELLED`. |
| **BR-PAY-02** | Các đăng ký lớp **sắp diễn ra** đã **trừ buổi vào đúng gói bị hoàn tiền** bị huỷ (`CANCELLED`), trả lại chỗ trong lớp. Đăng ký trừ vào gói khác của member không bị ảnh hưởng. |
| **BR-PAY-03** | Đăng ký cũ không gắn gói (`subscription = null`): chỉ huỷ khi giờ học nằm trong thời hạn gói bị hoàn tiền **và** member không còn gói `ACTIVE` nào khác phủ giờ học đó. |
| **BR-PAY-04** | Hoàn tiền **luôn** gửi 1 thông báo in-app cho member (`SUBSCRIPTION_REFUNDED`): tên gói, lý do hoàn tiền, và danh sách lớp bị huỷ theo (môn + giờ học) nếu có. Gói chưa đặt lớp nào vẫn nhận thông báo. |

### 3.6. Điểm danh

| Mã | Rule |
|---|---|
| **BR-ATT-01** | HLV chỉ điểm danh member đã đăng ký lớp (`MEMBER_NOT_IN_CLASS`). Member gói FREE tự nhiên không có trong danh sách. |
| **BR-ATT-02** | Điểm danh **không** trừ hay hoàn buổi. Buổi đã trừ lúc đăng ký → vắng mặt (ABSENT) thì mất buổi. |
| **BR-ATT-03** | HLV chỉ điểm danh / sửa điểm danh trong khoảng **từ 15 phút trước giờ học** (`app.attendance.open-minutes-before`) **đến 24 giờ sau khi lớp kết thúc** (`app.attendance.close-hours-after`). Sớm hơn → 422 `ATTENDANCE_TOO_EARLY`; muộn hơn → 422 `ATTENDANCE_CLOSED`. Lớp `CANCELLED` → 422 `ATTENDANCE_CLASS_NOT_ACTIVE`; lớp đã xoá mềm (`INACTIVE`) coi như không tồn tại → 404 `CLASS_NOT_FOUND`. Lớp `COMPLETED` vẫn điểm danh được trong khoảng trên. Xem điểm danh không bị giới hạn. |

### 3.7. Check-in tại quầy

| Mã | Rule |
|---|---|
| **BR-CHK-01** | Member có **ít nhất 1 gói ACTIVE còn hạn** (FREE hoặc CLASS) thì được check-in. Mỗi ngày 1 lần. |
| **BR-CHK-02** | Response check-in trả về **danh sách gói đang hiệu lực** (`activeSubscriptions`: loại, các môn kèm `areaAccess`, số buổi còn lại, ngày hết hạn) để lễ tân biết member được vào khu nào. Gói CLASS hết buổi nhưng còn hạn vẫn nằm trong danh sách (BR-SUB-03). |
| **BR-CHK-03** | Check-in **chung**, không theo môn. Khu `CLASS_ONLY` được kiểm soát bằng việc không bán gói FREE (BR-SUBJ-02) và lễ tân xem `areaAccess` trong response check-in. |
| **BR-CHK-04** | Check-in **không** trừ buổi. |

---

## 4. Luồng nghiệp vụ

### 4.1. Đăng ký lớp

```
Member chọn lớp Boxing 18:00 27/10
        │
Member tự đăng ký & chưa làm quiz? ──yes──► QUIZ_NOT_COMPLETED   (lễ tân đăng ký hộ bỏ qua bước này)
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
Khoá gói đã chọn, gói.usedSessions += 1
Lưu ClassRegistration(status=REGISTERED, subscription=gói đã chọn)
```

### 4.2. Huỷ đăng ký

```
Member huỷ đăng ký
        │
Đã CANCELLED? ──yes──► trả về, không làm gì
Quá cutoff? ──yes──► CANCEL_NOT_ALLOWED
        │
status = CANCELLED, cancelledAt = now
registration.subscription.usedSessions -= 1   (nếu đăng ký có gắn gói)
```

### 4.3. Mua gói (cảnh báo gói thừa)

```
Member chọn gói, bấm "Mua"
        │
        ▼
FE gọi preview (packageId)
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

### 4.4. Hoàn tiền gói

```
Manager hoàn tiền giao dịch đã thu
        │
payment.status = REFUNDED, subscription.status = CANCELLED
        │
Với mỗi đăng ký lớp sắp diễn ra của member:
  đăng ký trừ vào đúng gói này? ──yes──► CANCELLED (trả chỗ)
  đăng ký cũ không gắn gói, nằm trong hạn gói và không còn gói khác phủ giờ học? ──yes──► CANCELLED
  còn lại ──► giữ nguyên
```

### 4.5. Ví dụ end-to-end

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
| 14/11 | Đăng ký lớp Boxing 16/11 | 2 | |
| 15/11 | Trung tâm huỷ lớp Boxing 16/11 | 1 | Hoàn buổi |
| 20/11 | Đăng ký lớp Boxing 22/11, rồi được hoàn tiền gói Boxing | – | Gói Boxing `CANCELLED`, đăng ký 22/11 bị huỷ (BR-PAY-02). Gói Gym không bị ảnh hưởng |

> Nếu An không hoàn tiền và dùng hết 45 buổi Boxing trước 30/04 thì từ lúc đó không đăng ký lớp Boxing được nữa (`NO_SESSIONS_LEFT`), nhưng vẫn vào khu Boxing tự tập đến 30/04 (BR-SUB-03). Hết hạn khi còn buổi thì số buổi còn lại bị mất (BR-SUB-04).

---

## 5. Dữ liệu

| Bảng | Cột / bảng | Ghi chú |
|---|---|---|
| `subjects` | `area_access` (`OPEN`/`CLASS_ONLY`, NOT NULL, default `OPEN`) | BR-SUBJ-01 |
| `packages` | `package_type` (`FREE`/`CLASS`, NOT NULL) | Gói có sẵn trước đây mặc định thành `CLASS` |
| `packages` | `session_limit` (INT, NULL) | `null` = không giới hạn |
| `package_subjects` | `package_id`, `subject_id` | Quan hệ nhiều–nhiều gói ↔ môn |
| `subscriptions` | `used_sessions` (INT, NOT NULL, default 0) | |
| `class_registrations` | `subscription_id` (FK, NULL cho dữ liệu cũ) | Biết đăng ký trừ vào gói nào để hoàn buổi và xử lý hoàn tiền |

**Dữ liệu cũ:** gói có sẵn thành loại `CLASS` nhưng **chưa gắn môn**, nên không dùng được để đăng ký lớp cho tới khi Manager gắn môn. Team chọn reset dữ liệu test theo danh mục mới.

---

## 6. Code liên quan

| Chỗ | Nội dung |
|---|---|
| `Subject` + DTO + `SubjectServiceImpl` | `areaAccess`; chặn BR-SUBJ-03, BR-PKG-08 |
| `MembershipPackage` + `MembershipPackageRequestDTO` + `MembershipPackageServiceImp` | `packageType`, `sessionLimit`, `subjects`; validate BR-PKG-02…06, BR-SUBJ-02 |
| `SubscriptionRepository` | `findValidIdsForClass` (BR-REG-02, BR-REG-04), `findByIdForUpdate`, query gói trùng khu (BR-SUB-08) |
| `SubscriptionServiceImpl` | `calculateStartDate` (BR-SUB-05), `previewPurchase` (BR-SUB-08), `refundSession`, `getCurrentSubscriptions` (danh sách gói hiệu lực) |
| `SubscriptionController`, `ReceptionistController` | API preview cho member và lễ tân |
| `ClassRegistrationServiceImpl` | `registerSelf` (BR-REG-09), `register` (kiểm tra gói, trừ buổi), `cancel` (hoàn buổi) |
| `SportClassServiceImpl.cancelClass` / `deleteClass` | Hoàn buổi cho mọi đăng ký (BR-CAN-04) |
| `PaymentServiceImpl.refundPayment` | Huỷ đăng ký theo gói bị hoàn tiền (BR-PAY-01…03), gửi thông báo cho member (BR-PAY-04) |
| `UserServiceImpl.toggleUserStatus` / `updateUserStatus` | Khoá member → huỷ đăng ký sắp diễn ra, không hoàn buổi (BR-CAN-07) |
| `CheckInServiceImpl` | Trả `activeSubscriptions` kèm `areaAccess` (BR-CHK-02) |
| `ErrorCode` | `PACKAGE_NOT_ALLOW_CLASS`, `SUBJECT_NOT_COVERED`, `NO_SESSIONS_LEFT`, `FREE_PACKAGE_NOT_ALLOWED`, `PACKAGE_SUBJECT_REQUIRED`, `INVALID_PACKAGE_CONFIG`, `PACKAGE_IN_USE`, `SUBJECT_HAS_ACTIVE_FREE_PACKAGE`, `SUBJECT_HAS_ACTIVE_PACKAGE`, `QUIZ_NOT_COMPLETED` |
| `CoachServiceImpl.takeAttendance` | Kiểm tra khoảng thời gian điểm danh và trạng thái lớp (`ensureAttendanceWindow`, BR-ATT-03); không trừ/hoàn buổi |

---

## 7. Test case chính

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
| T10 | Đang có Yoga 12 buổi còn hạn, còn buổi, mua thêm Yoga 12 buổi | Gói mới nối sau `endDate` gói cũ |
| T11 | Gói còn ngày nhưng hết buổi, mua gói mới cùng môn | Gói mới bắt đầu ngay |
| T12 | Member gói FREE check-in | 201, response có danh sách gói |
| T13 | 2 request đăng ký cùng lúc khi gói còn 1 buổi | 1 thành công, 1 `NO_SESSIONS_LEFT` |
| T14 | Manager tạo gói FREE cho môn Boxing (`CLASS_ONLY`) | 400 `FREE_PACKAGE_NOT_ALLOWED` |
| T15 | Đổi môn Gym sang `CLASS_ONLY` khi còn gói FREE Gym `ACTIVE` | 409 `SUBJECT_HAS_ACTIVE_FREE_PACKAGE` |
| T16 | Gói CLASS Boxing hết buổi nhưng còn hạn, member check-in | 201, response vẫn có gói Boxing, số buổi còn lại = 0 |
| T17 | Đang có CLASS Gym, gọi preview mua FREE Gym | 200, có 1 cảnh báo kèm ngày hết hạn và số ngày còn lại |
| T18 | Đang có FREE Gym, gọi preview mua FREE Gym | 200, không cảnh báo, `startDate` = `endDate` gói cũ |
| T19 | Đang có FREE Gym, gọi preview mua CLASS Boxing | 200, không cảnh báo |
| T20 | Có gói CLASS Yoga + FREE Gym, đặt lớp Yoga bằng gói Yoga, rồi hoàn tiền gói Yoga | Gói Yoga `CANCELLED`, đăng ký lớp Yoga `CANCELLED`, gói Gym vẫn `ACTIVE` |
| T21 | Có 2 gói CLASS Yoga, mỗi gói đặt 1 lớp, hoàn tiền gói 1 | Chỉ lớp đặt bằng gói 1 bị huỷ |
| T22 | Member chưa làm quiz tự đăng ký lớp | 422 `QUIZ_NOT_COMPLETED`; lễ tân đăng ký hộ thì thành công |
| T23 | Sửa `sessionLimit` của gói đã có người mua | 409 `PACKAGE_IN_USE` |
| T24 | Xoá môn còn gói `ACTIVE` | 409 `SUBJECT_HAS_ACTIVE_PACKAGE` |
| T25 | Member gói CLASS 2 buổi đã đặt 1 lớp sắp diễn ra, Manager khoá member | Đăng ký `CANCELLED`, `usedSessions` giữ nguyên = 1 (không hoàn) |
| T26 | FREE Gym còn 2 ngày + CLASS Yoga còn 30 ngày, chạy job nhắc | Có thông báo cho gói Gym |
| T27 | FREE Gym còn 2 ngày + đã mua FREE Gym nối tiếp, chạy job nhắc | Không nhắc gói Gym cũ |
| T28 | HLV điểm danh lớp ngày mai | 422 `ATTENDANCE_TOO_EARLY` |
| T29 | HLV điểm danh lớp bắt đầu sau 10 phút / đang diễn ra / kết thúc 2 giờ trước | 200 |
| T30 | HLV điểm danh lớp kết thúc 2 ngày trước | 422 `ATTENDANCE_CLOSED` |
| T31 | HLV điểm danh lớp đã bị huỷ | 422 `ATTENDANCE_CLASS_NOT_ACTIVE` |
| T32 | Hoàn tiền gói có 2 lớp đã đặt | Member nhận 1 thông báo có tên gói, lý do, 2 lớp bị huỷ |
| T33 | Hoàn tiền gói chưa đặt lớp nào | Member vẫn nhận thông báo (không có phần lớp) |

---

## 8. Ngoài phạm vi (làm sau nếu còn thời gian)

- Nhắc khi gói CLASS sắp hết buổi (BR-SUB-07).
- Cộng thêm ngày cho gói khi lớp bị huỷ sát ngày hết hạn (BR-CAN-05).
- Check-in theo môn / khu vực (BR-CHK-03).
- Bảo lưu gói (tạm dừng, cộng thêm ngày).
- Hạn gói tính từ buổi học đầu tiên thay vì ngày thanh toán.
- Danh sách chờ khi lớp đầy.
- Phạt huỷ muộn / không đến (khoá quyền đặt lớp vài ngày).
- Lớp tốn nhiều hơn 1 buổi (workshop).
- Buổi học thử cho khách chưa mua gói.
- Chuyển nhượng gói cho người khác.
- Tạo lớp lặp lại theo lịch tuần (sinh nhiều `SportClass` một lần).
- Khoá học cố định (`Course` nhóm nhiều buổi, đăng ký cả khoá).

---

## 9. Đang xem xét (code hiện tại vẫn chạy theo mục 3)

### 9.1. Gói CLASS nối hạn có thể làm member bị kẹt (BR-SUB-05)
Ví dụ: ngày 01/11 member còn gói Yoga hạn tới 30/11, còn 3 buổi, và mua thêm gói Yoga thứ 2. Theo BR-SUB-05, gói 2 bắt đầu từ 30/11. Nếu ngày 05/11 member dùng hết 3 buổi còn lại, thì từ 05/11 tới 30/11 member **không đăng ký lớp được**, dù đã trả tiền gói 2.
Hướng đề xuất: gói CLASS **không nối hạn**, thanh toán xong là bắt đầu ngay; nhiều gói cùng hiệu lực thì BR-REG-04 đã tự trừ vào gói hết hạn sớm nhất. Gói FREE vẫn nối hạn như cũ.

### 9.2. Gói CLASS không giới hạn buổi (BR-PKG-05)
Gói CLASS có `sessionLimit = null` vẫn cho phép giữ chỗ nhiều lớp mà không tốn gì, tức vấn đề #2 ở mục 1 vẫn còn với loại gói này.
Hướng đề xuất (chọn một): bắt buộc `sessionLimit > 0` cho gói CLASS; hoặc giới hạn số đăng ký sắp diễn ra mỗi member (ví dụ tối đa 5).

### 9.3. Thông báo khi hoàn tiền (BR-PAY-04) — ✅ đã chốt 29/10/2026
Đã làm: xem BR-PAY-04.

---

## 10. Đã chốt

- [x] 2 loại gói FREE / CLASS (CLASS = FREE + đăng ký lớp)
- [x] BR-PKG-03: có gói FREE "toàn trung tâm", chỉ gồm khu `OPEN`
- [x] BR-SUBJ-01: `areaAccess` chỉ 2 giá trị `OPEN` / `CLASS_ONLY`
- [x] BR-SUB-03: hết buổi nhưng còn hạn vẫn vào khu tự tập
- [x] BR-SUB-08: mua gói thừa → cảnh báo trước khi thanh toán, không chặn, **không** bắt buộc `acknowledgedWarnings`
- [x] BR-SUBJ-03: đổi môn sang `CLASS_ONLY` khi còn gói FREE đang bán → **chặn**
- [x] BR-PKG-08: xoá môn còn gói đang bán → **chặn**
- [x] BR-SUB-07: **không** nhắc khi gói sắp hết buổi
- [x] BR-SUB-07: nhắc **từng gói**, chỉ bỏ qua khi đã gia hạn gói cùng loại + cùng môn (chốt 28/10/2026)
- [x] BR-CAN-05: lớp bị huỷ sát hạn gói **không** cộng ngày
- [x] BR-CHK-03: check-in **chung**
- [x] Cutoff huỷ đăng ký: **2 giờ**
- [x] Dữ liệu gói cũ: mặc định `CLASS`, team reset dữ liệu test
- [x] Làm **một lần** cả FREE/CLASS, đúng môn, nối hạn, số buổi, trừ/hoàn buổi
- [x] Hoàn tiền gói chỉ huỷ đăng ký trừ vào đúng gói đó (BR-PAY-02)
- [x] BR-CAN-07: khoá member → huỷ đăng ký sắp diễn ra, **không** hoàn buổi (chốt 28/10/2026)
- [x] BR-ATT-03: điểm danh từ 15 phút trước giờ học đến 24 giờ sau khi kết thúc, chặn lớp đã huỷ (chốt 28/10/2026)
- [ ] 9.1: gói CLASS có nối hạn không
- [ ] 9.2: có giữ gói CLASS không giới hạn buổi không
- [x] 9.3 / BR-PAY-04: hoàn tiền **luôn** gửi thông báo cho member, kèm lớp bị huỷ theo nếu có (chốt 29/10/2026)
