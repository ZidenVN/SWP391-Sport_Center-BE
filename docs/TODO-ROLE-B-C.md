# TODO Role B và Role C (rà soát ngày 02/11/2026)

Đối chiếu `main` (tới PR #69) với `docs/BE-TODO.md`. Compile xanh, 37 unit test đều đạt. Các mục dưới đây là chỗ **sai** hoặc **còn thiếu** so với nghiệp vụ đã chốt.

Mức độ: 🔴 lỗi, cần sửa trước khi demo · 🟠 lệch nghiệp vụ · 🟡 nên làm thêm.

---

## Role B: Lớp và buổi học

### B1 🔴 Dừng lớp không gửi thông báo cho hội viên
**File:** `service/impl/SportClassServiceImpl.java`, hàm `stopClass`

**Lỗi:** code chuyển các đăng ký sang `STOPPED` trước, rồi mới gọi `registeredMembers(id)`. Hàm này chỉ đọc đăng ký `REGISTERED`, nên danh sách rỗng và không ai nhận được thông báo. Hội viên không biết lớp đã dừng để chọn chuyển lớp hay nhận hoàn tiền.

**Sửa:** lấy danh sách người nhận **trước** khi đổi trạng thái.
```java
        // Lấy người nhận TRƯỚC khi đổi trạng thái (registeredMembers chỉ đọc REGISTERED)
        List<Member> members = registeredMembers(id);

        List<ClassRegistration> activeRegs = classRegistrationRepository.findActiveByClassId(id);
        for (ClassRegistration r : activeRegs) {
            r.setStatus("STOPPED");
        }
        classRegistrationRepository.saveAll(activeRegs);
```
Xoá dòng `List<Member> members = registeredMembers(id);` cũ ở phía dưới.

**Xong khi:** dừng lớp có 5 học viên thì cả 5 người nhận thông báo "Dừng lớp khẩn cấp".

---

### B2 🔴 Huỷ được lớp đã bắt đầu
**File:** `SportClassServiceImpl.cancelClass`

**Lỗi:** hàm chỉ kiểm tra `requireOpening`. Lớp đang học dở vẫn bị huỷ, và gói lớp của học viên bị trả về "chờ kích hoạt", nên họ được học thêm một khoá mới miễn phí. Nghiệp vụ quy định: huỷ chỉ áp dụng **trước khi lớp bắt đầu**; lớp đã bắt đầu thì phải dùng **Dừng lớp**.

**Sửa:** thêm mã lỗi vào `ErrorCode`:
```java
    CLASS_CANCEL_STARTED(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_CLASS_400_CANCEL_STARTED", "Lớp đã bắt đầu, hãy dùng chức năng Dừng lớp!"),
```
Trong `cancelClass`, ngay sau `requireOpening(sportClass);`:
```java
        LocalDateTime first = sportClass.getFirstSessionAt();
        if (first != null && !LocalDateTime.now().isBefore(first)) {
            throw new AppException(ErrorCode.CLASS_CANCEL_STARTED);
        }
```
Nên làm thêm điều kiện ngược lại cho `stopClass`: lớp **chưa** bắt đầu thì báo lỗi, yêu cầu dùng Huỷ lớp.

**Xong khi:** huỷ lớp đã có buổi diễn ra thì nhận 422 `ERR_CLASS_400_CANCEL_STARTED`, gói lớp của học viên giữ nguyên `ACTIVE`.

---

### B3 🔴 Tạo lớp không kiểm tra thời hạn gói đang bán
**File:** `SportClassServiceImpl.createClass` và `updateClass`

**Lỗi:** BE-TODO yêu cầu "thời hạn phải nằm trong `class-durations` của A", nhưng code chưa kiểm tra. Manager tạo được lớp 45 ngày trong khi chỉ có gói 30 và 90 ngày. C chỉ cho đăng ký khi thời hạn gói **bằng** thời hạn lớp, nên lớp đó **không ai đăng ký được**.

**Sửa:** inject `MembershipPackageService` (A đã viết sẵn `getClassDurations`) và thêm mã lỗi:
```java
    CLASS_DURATION_NOT_SOLD(HttpStatus.BAD_REQUEST, "ERR_CLASS_400_DURATION", "Môn này chưa có gói lớp với thời hạn đã chọn!"),
```
Trong nhánh tạo lớp lặp lịch, sau khi kiểm tra `startDate`:
```java
            if (!membershipPackageService.getClassDurations(subject.getSubjectId()).contains(requestDTO.getDurationDays())) {
                throw new AppException(ErrorCode.CLASS_DURATION_NOT_SOLD);
            }
```
Thêm cùng điều kiện vào `updateClass`, ở trường hợp được phép đổi lịch. Không có rủi ro phụ thuộc vòng tròn: `MembershipPackageServiceImp` không inject service nào của lớp.

**Xong khi:** tạo lớp Boxing 45 ngày khi chỉ có gói Boxing 30/90 ngày thì nhận 400 `ERR_CLASS_400_DURATION`.

---

### B4 🟠 Ngày bắt đầu lớp đang cho phép là hôm nay
**File:** `SportClassServiceImpl`, dòng khoảng 75 (`createClass`) và 404 (`updateClass`)

**Lỗi:** `isBefore(LocalDate.now())` cho phép bắt đầu ngay hôm nay. Nếu tạo lúc 10h mà lịch học là 8h sáng thì lớp coi như **đã bắt đầu** ngay khi vừa tạo, và không ai đăng ký được. Nghiệp vụ: "ngày bắt đầu ở tương lai".

**Sửa:**
```java
if (!requestDTO.getStartDate().isAfter(LocalDate.now())) {   // từ ngày mai trở đi
    throw new AppException(ErrorCode.CLASS_START_DATE_INVALID);
}
```

---

### B5 🔴 Lớp dư 1 ngày học
**File:** `SportClassServiceImpl`, dòng khoảng 81 (`createClass`) và 413 (`updateClass`)

**Lỗi:**
```java
LocalDate endDate = requestDTO.getStartDate().plusDays(requestDTO.getDurationDays());
for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {   // có tính cả endDate
```
Lớp 30 ngày bắt đầu 01/12 thì `endDate = 31/12`, và vòng lặp tính cả ngày 31/12. Lớp kéo dài **31 ngày**, có thể dư 1 buổi.

**Sửa:** thống nhất `endDate` là **ngày học cuối cùng, tính luôn ngày đó**:
```java
// 30 ngày từ 01/12 -> ngày học cuối là 30/12
LocalDate endDate = requestDTO.getStartDate().plusDays(requestDTO.getDurationDays() - 1);
```
Sau khi sửa, cả hệ thống dùng chung một cách hiểu:
- Buổi bù sau ngày kết thúc thì `endDate = ngày buổi bù` (`ClassSessionServiceImpl` đã làm đúng).
- A tính hạn gói lớp = `endDate + 1 ngày lúc 00:00` (`activateClassPackage`, `extendClassPackages` đã làm đúng).

**Xong khi:** lớp T2-T4-T6 30 ngày, bắt đầu thứ Hai, có `endDate` là ngày thứ 30, và không có buổi nào sau ngày đó.

---

### B6 🟡 Báo trùng lịch chưa nói rõ trùng ở buổi nào
BE-TODO yêu cầu "trùng 1 buổi thì chặn cả lớp **và báo rõ buổi nào**". Hiện `SESSION_SCHEDULE_CONFLICT` chỉ là một câu chung. Nên trả kèm ngày giờ của buổi bị trùng, và việc này phòng hay HLV, để Manager biết đường sửa.

---

### B7 🟡 Report sự cố: Manager vẫn gửi được report
**File:** `service/impl/IssueReportServiceImpl.java`, dòng khoảng 52

Nghiệp vụ là "Manager không có nút gửi, chỉ xử lý". Code đang cho Manager gửi mọi danh mục.
```java
        boolean roleAllowed = userRole.equalsIgnoreCase(categoryRole);   // bỏ "|| CENTER_MANAGER"
```
Nhớ sửa lại unit test tương ứng trong `IssueReportServiceImplTest` nếu có.

---

## Role C: HLV, đăng ký, đổi lớp

### C1 🔴 Chuyên môn HLV (`coach_subjects`): chưa làm
**Nghiệp vụ:** mỗi HLV có ít nhất 1 môn, không giới hạn số môn. Mọi chỗ gán HLV đều phải đúng môn. Bỏ môn khỏi HLV khi còn lớp chưa kết thúc của môn đó thì chặn.

**Hiện trạng:** `Coach` vẫn chỉ có `speciality` (chữ tự do). `guard/CoachSubjectGuard` đang **so chữ**: "contains", token từ 3 ký tự, chữ "fitness" khớp mọi môn. Vì vậy HLV mặc định "General Fitness" **dạy được mọi môn**.

**Việc cần làm:**
1. **`entity/Coach.java`**: thêm `subjects`. Bỏ `nullable = false` của `speciality` và gắn `@Deprecated`.
   ```java
       @ManyToMany(fetch = FetchType.LAZY)
       @JoinTable(name = "coach_subjects",
           joinColumns = @JoinColumn(name = "coach_id"),
           inverseJoinColumns = @JoinColumn(name = "subject_id"))
       @Builder.Default
       private Set<Subject> subjects = new HashSet<>();
   ```
2. **DTO:** `CoachRequestDTO` và `CoachUpdateRequestDTO` thêm:
   ```java
       @NotEmpty(message = "HLV phải có ít nhất 1 môn chuyên môn")
       private Set<Long> subjectIds;
   ```
   `CoachResponseDTO` trả thêm danh sách môn (`subjectId`, `subjectName`).
3. **`CoachServiceImpl`:**
   - Tạo và sửa HLV: tải các môn ACTIVE theo `subjectIds`. Id nào không có thì báo `SUBJECT_NOT_FOUND`.
   - Khi sửa: những môn **bị bỏ đi** mà HLV còn lớp `OPENING` của môn đó thì chặn:
     ```java
         COACH_SUBJECT_IN_USE(HttpStatus.CONFLICT, "ERR_COACH_409_SUBJECT", "HLV còn lớp chưa kết thúc của môn này, hãy đổi HLV cho lớp trước!"),
     ```
4. **Lọc HLV theo môn:** `GET /coaches?subjectId=` để FE hiện đúng HLV khi tạo lớp.
   ```java
       List<Coach> findDistinctBySubjects_SubjectId(Long subjectId);   // CoachRepository
   ```
5. **`guard/CoachSubjectGuard`** (file của B, **báo B trước khi sửa**): bỏ cách so chữ, chuyển sang so id:
   ```java
           boolean matches = coach.getSubjects().stream()
               .anyMatch(s -> s.getSubjectId().equals(subject.getSubjectId()));
   ```
   Sửa lại `CoachSubjectGuardTest`. Hàm phải được gọi trong `@Transactional`, vì `subjects` là LAZY.
6. **Dữ liệu đã có** (DB local, production): gán môn theo `speciality` hiện tại.
   ```sql
   INSERT INTO coach_subjects (coach_id, subject_id)
   SELECT c.coach_id, s.subject_id FROM coaches c JOIN subjects s ON s.subject_name = c.speciality;
   ```
   HLV mặc định có speciality "General Fitness" không khớp môn nào, cần gán tay (ví dụ Gym).

**Xong khi:**
- Tạo HLV không chọn môn thì nhận 400.
- Gán HLV Yoga vào lớp Boxing thì nhận lỗi `COACH_NOT_TEACH_SUBJECT`, kể cả với HLV "General Fitness".
- Bỏ môn Boxing của một HLV đang dạy lớp Boxing thì nhận 409.

---

### C2 🟠 Lễ tân đổi lớp: chỉ tính lượt khi lớp đã bắt đầu
**File:** `service/impl/ClassRegistrationServiceImpl.java`, dòng 204

**Nghiệp vụ (chốt 02/11):**

| Thời điểm (so với buổi học đầu) | Hội viên tự đổi | Lễ tân đổi giúp |
|---|---|---|
| Còn hơn 12 tiếng | Được, không tính lượt | Được, không tính lượt |
| Từ 12 tiếng trước tới lúc bắt đầu | Không được | Được, **không tính lượt** |
| Lớp đã bắt đầu | Không được | Được, **tính 1 lượt** (tối đa 1 lần mỗi gói lớp) |
| Lớp bị dừng | Không được | Được, không tính lượt |

**Lỗi:** code đang tính lượt ngay khi quá mốc 12 tiếng, kể cả khi lớp chưa bắt đầu.

**Sửa:**
```java
        } else if (REGISTERED.equals(current.getStatus())) {
            // Chỉ tính lượt khi lớp đã bắt đầu; trước đó lễ tân đổi giúp không tính lượt
            countTurn = hasStarted(current.getSportClass(), now);
```
`hasStarted` đã có sẵn trong file.

**Xong khi:** chỉ trường hợp "lớp đã bắt đầu" làm `receptionistSwitchCount` tăng lên 1, và lần đổi thứ hai sau đó bị chặn với `REGISTRATION_SWITCH_LIMIT`.

---

### C3 🟡 Seed dữ liệu trong `DataInitializer`
BE-TODO có mục "DataInitializer seed quyền mới, vài gói MEMBERSHIP/CLASS, HLV có môn". Hiện chưa làm. Sau khi xong C1, HLV mặc định nên được gán ít nhất 1 môn (khi môn đã tồn tại), để tạo lớp không bị chặn.

---

## Chung cho cả team
- **DB production (Railway):** khi deploy bản mới cần **reset DB**, hoặc chạy migrate. Gói loại `FREE` cũ khiến `GET /packages` lỗi 500 (`No enum constant PackageType.FREE`). Thêm vào đó còn các cột, bảng thừa `subscriptions.used_sessions`, `packages.session_limit`, `package_subjects`, và bảng `coach_subjects` cần dữ liệu từ bước C1.6.
- Thứ tự gợi ý: **B1, B2, B3, B5** (lỗi ảnh hưởng dữ liệu), rồi **C1, C2**, cuối cùng là các mục 🟡.
