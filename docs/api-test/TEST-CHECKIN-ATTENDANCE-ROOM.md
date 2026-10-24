# Test nhanh: Lịch sử điểm danh, Check-in tại quầy, Bảo trì phòng

Checklist test tay bằng Postman cho 3 thay đổi:

| Phần | API |
|---|---|
| A. Lịch sử điểm danh (member) | `GET /api/v1/members/me/attendances` |
| B. Check-in tại quầy (lễ tân) | `POST /api/v1/receptionist/members/{memberId}/check-ins`, `GET /api/v1/receptionist/check-ins` |
| C. Chặn chuyển phòng sang bảo trì khi còn lớp | `PUT /api/v1/rooms/{id}` |

Cả 3 phần có thể chạy tự động bằng collection [`CheckIn-Attendance-Room.postman_collection.json`](CheckIn-Attendance-Room.postman_collection.json): Import → tab **Variables** điền `managerPassword`, `receptionistPassword` → **Run**.

## Chuẩn bị

1. `docker start sports_center_mysql`
2. **Restart backend** (bảng `check_ins` được Hibernate tự tạo khi khởi động).
3. Trong Postman tạo Environment với các biến dưới đây, điền dần trong lúc test:

| Biến | Lấy từ |
|---|---|
| `base_url` | `http://localhost:8080` |
| `managerToken`, `receptionistToken`, `coachToken`, `memberToken` | Bước 0.1, 0.6, 0.7 |
| `packageId`, `subjectId`, `coachId` | Bước 0.2, 0.3, 0.7 |
| `memberId`, `memberNoSubId`, `paymentId` | Bước 0.4, 0.5 |
| `roomP`, `roomQ`, `classId` | Bước 0.3, 0.8 |

Mỗi request: **Authorization → Bearer Token** = token ghi trong cột *Token*. Request có body: **Body → raw → JSON**.

Khi test lại lần sau, đổi các giá trị có hậu tố `01` (email, SĐT, tên môn/phòng) sang số khác để không bị trùng.

---

## 0. Chuẩn bị dữ liệu

**0.1 Đăng nhập Manager và Lễ tân** (không token) → lưu `token` vào `managerToken`, `receptionistToken`

`POST {{base_url}}/api/v1/auth/login`
```json
{ "email": "manager@sportcenter.com", "password": "<mật khẩu manager>" }
```
```json
{ "email": "receptionist@Sportcenter.com", "password": "<mật khẩu lễ tân>" }
```

**0.2 Tạo gói tập** (manager) → `packageId`

`POST {{base_url}}/api/v1/packages`
```json
{
  "packageName": "Gói test 01",
  "durationDays": 30,
  "description": "Gói dùng để test",
  "price": 100000
}
```

**0.3 Tạo môn và 2 phòng** (manager) → `subjectId`, `roomP`, `roomQ`

`POST {{base_url}}/api/v1/subjects`
```json
{ "subjectName": "Môn test 01", "description": "Môn dùng để test" }
```
`POST {{base_url}}/api/v1/rooms` (gọi 2 lần)
```json
{ "roomName": "Phòng P test 01", "capacity": 20 }
```
```json
{ "roomName": "Phòng Q test 01", "capacity": 20 }
```

**0.4 Tạo hội viên 1 (sẽ mua gói)** (lễ tân) → `member.memberId` vào `memberId`, ghi lại `temporaryPassword`

`POST {{base_url}}/api/v1/receptionist/members`
```json
{
  "firstName": "An",
  "lastName": "Nguyễn Văn",
  "gender": "MALE",
  "email": "test.member01@example.com",
  "phone": "0901000001",
  "trainingGoal": "Test"
}
```

**0.5 Tạo hội viên 2 (không mua gói)** (lễ tân) → `memberNoSubId`

`POST {{base_url}}/api/v1/receptionist/members`
```json
{
  "firstName": "Bình",
  "lastName": "Trần",
  "gender": "FEMALE",
  "email": "test.nosub01@example.com",
  "phone": "0902000001",
  "trainingGoal": "Test"
}
```

**0.6 Bán gói cho hội viên 1 và xác nhận thu tiền** (lễ tân)

`POST {{base_url}}/api/v1/receptionist/members/{{memberId}}/subscriptions` → `paymentId`
```json
{ "packageId": {{packageId}}, "paymentMethod": "CASH" }
```
`PATCH {{base_url}}/api/v1/receptionist/payments/{{paymentId}}/confirm` → `status: "COMPLETED"`
```json
{ "note": "Thu tiền mặt" }
```
Đăng nhập hội viên 1 (không token) → `memberToken`

`POST {{base_url}}/api/v1/auth/login`
```json
{ "email": "test.member01@example.com", "password": "<temporaryPassword ở bước 0.4>" }
```

**0.7 Tạo Coach và đăng nhập** (manager) → `coachId`, `coachToken`

`POST {{base_url}}/api/v1/coaches`
```json
{
  "firstName": "Cường",
  "lastName": "Lê",
  "gender": "MALE",
  "phone": "0903000001",
  "email": "test.coach01@example.com",
  "password": "QaTest@12345",
  "specialty": "Yoga"
}
```
`POST {{base_url}}/api/v1/auth/login` (không token)
```json
{ "email": "test.coach01@example.com", "password": "QaTest@12345" }
```

**0.8 Tạo lớp ở phòng P, hội viên đặt lớp, coach điểm danh**

`POST {{base_url}}/api/v1/classes` (manager) → `classId`. Giờ học phải ở tương lai, đổi ngày nếu cần.
```json
{
  "maxCapacity": 10,
  "startTime": "2026-10-27T09:00:00",
  "endTime": "2026-10-27T10:00:00",
  "coachId": {{coachId}},
  "roomId": {{roomP}},
  "subjectId": {{subjectId}}
}
```
`POST {{base_url}}/api/v1/members/me/registrations` (member)
```json
{ "classId": {{classId}} }
```
`POST {{base_url}}/api/v1/coaches/me/classes/{{classId}}/attendances` (coach)
```json
{
  "records": [
    { "memberId": {{memberId}}, "status": "PRESENT", "note": "Đi đúng giờ" }
  ]
}
```

---

## A. Lịch sử điểm danh

| # | Request | Token | Mong đợi | ✓ |
|---|---|---|---|---|
| A1 | `GET /api/v1/members/me/attendances` | member | **200**. Có phần tử với `classId = {{classId}}`, `status: "PRESENT"`, `statusDescription: "Có mặt"`, `note: "Đi đúng giờ"`, có `subjectName`, `coachName`, `roomName`, `startTime`, `markedAt` | ☐ |
| A2 | Như A1, đăng nhập hội viên khác chưa từng được điểm danh | member khác | **200**, `[]` | ☐ |
| A3 | Như A1 | coach | **403** | ☐ |
| A4 | Như A1 | receptionist | **403** | ☐ |

## B. Check-in tại quầy (không có body)

| # | Request | Token | Mong đợi | ✓ |
|---|---|---|---|---|
| B1 | `POST /api/v1/receptionist/members/{{memberId}}/check-ins` | receptionist | **201**. Có `checkInId`, `memberName`, `phone: "0901000001"`, `receptionistName`, `packageName: "Gói test 01"`, `subscriptionEndDate` | ☐ |
| B2 | Gọi lại B1 | receptionist | **409** `ERR_CHECKIN_001` | ☐ |
| B3 | `POST /api/v1/receptionist/members/{{memberNoSubId}}/check-ins` | receptionist | Bị chặn: `NO_ACTIVE_SUBSCRIPTION` | ☐ |
| B4 | `POST /api/v1/receptionist/members/999999/check-ins` | receptionist | **404** | ☐ |
| B5a | `PATCH /api/v1/users/{{memberId}}/status` (khoá) | manager | **200** | ☐ |
| B5b | Gọi lại B1 | receptionist | **400** `ERR_CHECKIN_002` | ☐ |
| B5c | `PATCH /api/v1/users/{{memberId}}/status` (mở khoá) | manager | **200** | ☐ |
| B6 | `GET /api/v1/receptionist/check-ins` | receptionist | **200**, có lượt check-in của B1, mới nhất trước | ☐ |
| B7 | `GET /api/v1/receptionist/check-ins?date=2020-01-01` | receptionist | **200**, `[]` | ☐ |
| B8 | Gọi B1 | member | **403** | ☐ |
| B9 | Gọi B6 | coach | **403** | ☐ |
| B10 | `GET /api/v1/admin/audit-logs?actionType=CHECK_IN` | manager | Có dòng `actionType: "CHECK_IN"`, `targetEntityId = {{memberId}}` | ☐ |

## C. Bảo trì phòng

Quy tắc: chỉ chuyển phòng sang `MAINTENANCE` được khi phòng không còn lớp `OPENING` sắp diễn ra. Phòng P đang có lớp `{{classId}}`, phòng Q không có lớp nào.

**C1.** Phòng P (có lớp) → bảo trì. Mong đợi **409** `ERR_ROOM_003` ☐

`PUT {{base_url}}/api/v1/rooms/{{roomP}}` (manager)
```json
{ "roomName": "Phòng P test 01", "capacity": 20, "status": "MAINTENANCE" }
```

**C2.** Phòng P đổi tên, giữ AVAILABLE. Mong đợi **200**, tên mới ☐

`PUT {{base_url}}/api/v1/rooms/{{roomP}}`
```json
{ "roomName": "Phòng P test 01 (sửa)", "capacity": 20, "status": "AVAILABLE" }
```

**C3.** Phòng Q (không có lớp) → bảo trì. Mong đợi **200**, `status: "MAINTENANCE"` ☐

`PUT {{base_url}}/api/v1/rooms/{{roomQ}}`
```json
{ "roomName": "Phòng Q test 01", "capacity": 20, "status": "MAINTENANCE" }
```

**C4.** Phòng Q đang bảo trì, chỉ đổi tên. Mong đợi **200** (không bị chặn nhầm) ☐

`PUT {{base_url}}/api/v1/rooms/{{roomQ}}`
```json
{ "roomName": "Phòng Q test 01 (sửa)", "capacity": 20, "status": "MAINTENANCE" }
```

**C5.** Tạo lớp ở phòng Q đang bảo trì. Mong đợi **400** `ERR_ROOM_001` ☐

`POST {{base_url}}/api/v1/classes`
```json
{
  "maxCapacity": 10,
  "startTime": "2026-10-28T09:00:00",
  "endTime": "2026-10-28T10:00:00",
  "coachId": {{coachId}},
  "roomId": {{roomQ}},
  "subjectId": {{subjectId}}
}
```

**C6.** Status không hợp lệ. Mong đợi **400** "Trạng thái phòng chỉ nhận AVAILABLE hoặc MAINTENANCE" ☐

`PUT {{base_url}}/api/v1/rooms/{{roomQ}}`
```json
{ "roomName": "Phòng Q test 01", "capacity": 20, "status": "INACTIVE" }
```

**C7.** Phòng Q về AVAILABLE. Mong đợi **200** ☐

`PUT {{base_url}}/api/v1/rooms/{{roomQ}}`
```json
{ "roomName": "Phòng Q test 01", "capacity": 20, "status": "AVAILABLE" }
```

**C8.** Huỷ lớp ở phòng P rồi chuyển P sang bảo trì. Mong đợi cả 2 request **200** ☐

`PATCH {{base_url}}/api/v1/classes/{{classId}}/cancel`
```json
{ "reason": "Test bảo trì phòng" }
```
`PUT {{base_url}}/api/v1/rooms/{{roomP}}`
```json
{ "roomName": "Phòng P test 01", "capacity": 20, "status": "MAINTENANCE" }
```

---

## Dọn dữ liệu (manager, không body)

| Request | Ghi chú |
|---|---|
| `DELETE /api/v1/classes/{{classId}}` | |
| `DELETE /api/v1/coaches/{{coachId}}` | Khoá coach test |
| `PATCH /api/v1/users/{{memberId}}/status` | Khoá hội viên 1 |
| `PATCH /api/v1/users/{{memberNoSubId}}/status` | Khoá hội viên 2 |
| `DELETE /api/v1/rooms/{{roomP}}`, `DELETE /api/v1/rooms/{{roomQ}}` | |
| `DELETE /api/v1/subjects/{{subjectId}}` | |
| `DELETE /api/v1/packages/{{packageId}}` | |

Request nào sai kết quả: chụp lại response (status + JSON) để đối chiếu với cột *Mong đợi*.
