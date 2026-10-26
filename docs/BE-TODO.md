# Việc cần làm phía Backend

Đối chiếu ngày 25/10/2026 trên branch `hotfix` (commit `856558d`).
Bộ test tự động hiện **pass 142/142** (`docs/api-test/SportsCenter-Full-API.postman_collection.json`), nghĩa là không có API nào crash trong các luồng đã test. Danh sách dưới đây là **chức năng còn thiếu** và **quy tắc nghiệp vụ còn sai**.

## Tổng hợp

| # | Việc | Nhóm | Ưu tiên | Frontend đang chờ? |
|---|---|---|---|---|
| 1 | `GET /receptionist/members` cho phép `keyword` rỗng | Thiếu API | 🔴 | ✅ (FE-TODO #8) |
| 2 | `PUT /receptionist/members/{memberId}`: Lễ tân sửa hội viên | Thiếu API | 🔴 | ✅ (FE-TODO #9) |
| 3 | `GET` / `PUT /users/me`: hồ sơ nhân viên | Thiếu API | 🔴 | ✅ (FE-TODO #10) |
| 4 | Lịch sử check-in theo hội viên (2 API) | Thiếu API | 🟠 | ✅ (FE-TODO #11) |
| 5 | Trả `memberPlanId` trong danh sách giáo án | Thiếu API | 🟠 | ✅ (FE-TODO #12) |
| 6 | Bỏ N+1 khi xác thực (10–28 câu SQL mỗi request) | Hiệu năng | 🔴 | |
| 7 | Khoá Coach qua `/users/{id}/status` bỏ qua kiểm tra lớp sắp dạy | Sai nghiệp vụ | 🔴 | |
| 8 | Điểm danh được cả lớp chưa diễn ra | Sai nghiệp vụ | 🟠 | |
| 9 | Hồ sơ hội viên nhận giới tính bất kỳ | Sai nghiệp vụ | 🟡 | |
| 10 | Response check-in luôn `null` ở `packageName`, `subscriptionEndDate` | Sai dữ liệu | 🟡 | |
| 11 | Hoàn tiền nhưng hội viên vẫn giữ chỗ các lớp đã đặt | Sai nghiệp vụ | 🔴 | |
| 12 | Khoá Member nhưng đăng ký lớp vẫn chiếm chỗ | Sai nghiệp vụ | 🟠 | |
| 13 | Mô hình gói tập theo buổi (`docs/BUSINESS-GOI-TAP-THEO-BUOI.md`) | Thiết kế | ❓ Chờ chốt | |

**Thứ tự đề xuất:** 1 → 5 (frontend đang chờ) → 6 → 7, 8, 9, 10 → **chốt mục 13 với nhóm** → 11, 12 (phụ thuộc mô hình gói).

---

## Chi tiết

### 1. Lễ tân xem toàn bộ hội viên

- `ReceptionistController.search`: `@RequestParam String keyword` → `@RequestParam(defaultValue = "") String keyword`.
- `ReceptionistServiceImpl.searchMembers`: bỏ `if (keyword blank) return List.of();`, dùng `memberRepository.search(keyword == null ? "" : keyword.trim())`. Query hiện có với `''` sẽ khớp mọi hội viên.

**Test:** Lễ tân `GET /api/v1/receptionist/members` (không có keyword) → 200, đủ hội viên.

### 2. Lễ tân sửa thông tin hội viên

- `ReceptionistController`: thêm `private final MemberService memberService;` và
  `PUT /{memberId}` (`hasRole('RECEPTIONIST') and hasAuthority('UPDATE_USER')`) gọi `memberService.updateProfile(memberId, dto)` với body `MemberProfileUpdateDTO`.
- Dùng lại logic sẵn có: chặn trùng SĐT, id không phải hội viên thì trả 404.

**Test:** sửa hội viên → 200; dùng id của Manager → 404 `ERR_MEMBER_404`.

### 3. Hồ sơ nhân viên `/users/me`

- `UserController`: `GET /me` và `PUT /me`, đều `@PreAuthorize("isAuthenticated()")`.
  - `GET`: `userService.getUserById(principal.getUser().getUserId())`.
  - `PUT`: body `UserUpdateRequestDTO`, **gọi `requestDTO.setEmail(null)` trước** rồi `userService.updateUser(...)`. Email là tên đăng nhập và nằm trong JWT; tự đổi email sẽ làm token đang dùng mất hiệu lực.

**Test:** Lễ tân `GET /users/me` → 200; Coach `PUT /users/me {"email":"x@y.com"}` → 200 nhưng email không đổi; không token → 401.

### 4. Lịch sử check-in

- `CheckInRepository`: `findByMemberId(memberId)` (JOIN FETCH member, LEFT JOIN FETCH receptionist, mới nhất trước).
- `CheckInService` / `Impl`: `getMemberCheckIns(memberId)`, báo `MEMBER_NOT_FOUND` nếu không tồn tại.
- `CheckInController`: `GET /receptionist/members/{memberId}/check-ins` (`CHECK_IN_MEMBER`).
- `MemberController`: `GET /members/me/check-ins` (`hasRole('MEMBER')`).

**Test:** check-in 1 hội viên → cả 2 API đều trả lượt đó; id không tồn tại → 404.

### 5. `memberPlanId` trong danh sách giáo án

- `WorkoutPlanResponseDTO`: thêm `private Long memberPlanId;`.
- `WorkoutPlanServiceImpl.getPlansByMember`: thêm `.memberPlanId(mp.getMemberPlanId())`.

**Test:** `GET /coaches/members/{id}/workout-plans` có `memberPlanId` → `PATCH /coaches/member-plans/{memberPlanId}/status?status=COMPLETED` → 200.

### 6. Bỏ N+1 khi xác thực

`CustomUserDetailsService` dòng 33 gọi `rp.getPermission()` trong vòng lặp. `RolePermission.permission` là LAZY nên **mỗi quyền tốn thêm 1 câu SQL**, và việc này chạy ở **mọi request**. Manager có khoảng 25 quyền, tức khoảng 28 câu SQL; với DB trên Railway mỗi câu khoảng 250 ms.

- `RolePermissionRepository`: thêm
  ```java
  @Query("SELECT p.permissionName FROM RolePermission rp JOIN rp.permission p WHERE rp.role.roleId = :roleId")
  List<String> findPermissionNamesByRoleId(@Param("roleId") Long roleId);
  ```
- `CustomUserDetailsService`: thay khối lấy quyền bằng `permissionNames = rolePermissionRepository.findPermissionNamesByRoleId(roleId);`.

**Test:** chạy lại bộ test 142 request → vẫn pass. Bật `hibernate.generate_statistics=true` để thấy số câu SQL mỗi request giảm còn khoảng 3.

### 7. Khoá Coach bỏ qua kiểm tra lớp sắp dạy

`UserServiceImpl.toggleUserStatus` khoá được Coach đang có lớp `OPENING` sắp diễn ra. Kiểm tra `COACH_HAS_UPCOMING_CLASSES` hiện chỉ nằm ở `DELETE /coaches/{id}`.

- Khi user là `Coach` và đang chuyển sang `INACTIVE`: nếu `sportClassRepository.existsByCoach_UserIdAndStatusAndStartTimeAfter(id, "OPENING", now)` thì ném `COACH_HAS_UPCOMING_CLASSES`.

**Test:** Coach có lớp tương lai → `PATCH /users/{coachId}/status` → 409 `ERR_COACH_409`.

### 8. Điểm danh được cả lớp chưa diễn ra

`CoachServiceImpl.takeAttendance` không kiểm tra thời gian, nên điểm danh trước được cho lớp tuần sau và làm sai số buổi trong báo cáo, tiến độ.

- Chỉ cho điểm danh khi `now >= startTime` (có thể nới thêm khoảng 15 phút trước giờ học). Nếu chưa tới giờ thì trả mã lỗi 422 mới, ví dụ `ATTENDANCE_TOO_EARLY`.

**Test:** điểm danh lớp ngày mai → 422; lớp đang diễn ra → 200.

### 9. Giới tính trong hồ sơ hội viên

`MemberProfileUpdateDTO.gender` chỉ có `@NotBlank`. Thêm `@Pattern(regexp = "^(MALE|FEMALE|OTHER)$")` cho giống các DTO khác.

**Test:** `PUT /members/me` với `"gender": "abc"` → 400.

### 10. Response check-in thiếu thông tin gói

`CheckInResponseDTO` có `packageName` và `subscriptionEndDate`, nhưng `CheckInServiceImpl.toResponse` **không gán** 2 trường này nên luôn là `null`. Frontend cần chúng để hiển thị gói của hội viên khi check-in.

- Lấy gói hiện tại (`subscriptionRepository.findCurrent(memberId, checkInTime)`) và gán `packageName`, `subscriptionEndDate`.

**Test:** check-in → response có tên gói và ngày hết hạn.

### 11. Hoàn tiền nhưng vẫn giữ chỗ lớp ⚠️ phụ thuộc mục 13

`PaymentServiceImpl.refundPayment` chuyển gói sang `CANCELLED`, nhưng các `ClassRegistration` (`REGISTERED`) của lớp **chưa diễn ra** trong thời gian của gói đó vẫn giữ nguyên.

- Sau khi huỷ gói: huỷ các đăng ký có `startTime` nằm trong khoảng của gói và **không còn gói ACTIVE nào khác phủ thời điểm đó** (dùng `existsActiveCovering`).
- Cần chốt thêm: gói gia hạn nối tiếp (gói sau) có dời lên thay gói bị hoàn tiền không (D-14).

### 12. Khoá Member nhưng đăng ký vẫn chiếm chỗ ⚠️ phụ thuộc mục 13

Khi khoá Member (`toggleUserStatus` sang `INACTIVE`): huỷ các đăng ký `REGISTERED` ở lớp chưa diễn ra. Khi mở khoá **không** tự đăng ký lại.

### 13. Mô hình gói tập theo buổi ❓

`docs/BUSINESS-GOI-TAP-THEO-BUOI.md` (DRAFT) đề xuất gói `FREE` / `CLASS`, giới hạn số buổi, gói gắn với môn. Nội dung này **thay đổi logic** đăng ký lớp, check-in, gia hạn gói và hoàn tiền. Cần nhóm chốt các mục **[CẦN CHỐT]** trước khi làm mục 11, 12, để không phải sửa hai lần.

---

## Quy trình cho mỗi mục

1. Code trên branch `hotfix` (không commit thẳng `main`).
2. `Cmd + F9` không lỗi → dừng hẳn và Run lại backend (trỏ DB Docker).
3. Test theo phần **Test** của mục đó, rồi chạy lại toàn bộ collection → phải vẫn **142/142**.
4. Commit → push `hotfix` → PR vào `main`. Ghi trong PR nếu frontend cần sửa theo.
