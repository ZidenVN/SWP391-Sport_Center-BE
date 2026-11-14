-- =====================================================================================
-- Lớp Pickleball ĐANG HỌC để demo điểm danh buổi 13:00–14:30 hôm nay (MySQL 8.0+).
--
--   Pickleball · T3-T5-T7 13:00–14:30 · 03/11 → 02/12/2026 (30 ngày) · HLV Michael Carrick · Sân Pickleball
--   Học viên: 4 member có sẵn đang có membership ACTIVE (chinhnpse170133, nhanttse200464, truongdu9805, chinh@gmail)
--   Buổi 03, 05, 07, 10, 12/11: DONE + đã điểm danh. Buổi 14/11 13:00: SCHEDULED, CHƯA điểm danh -> HLV điểm danh lúc demo.
--   Mỗi học viên: gói lớp "Pickleball 30 ngày" đã thanh toán tại quầy + đăng ký lớp + điểm danh các buổi đã qua.
--
-- Giờ trong DB là giờ VN (giống các lớp tạo từ app). Không dùng NOW() vì Aiven chạy giờ UTC.
-- Điểm danh mở từ 12:45 (15 phút trước giờ học), đóng sau 24h.
--
-- An toàn: chỉ THÊM dữ liệu, chạy trong 1 transaction. Dừng (không ghi gì) nếu:
--   thiếu dữ liệu nền, đã chạy rồi, member đã đăng ký lớp này, hoặc HLV/phòng/học viên bị trùng lịch.
-- Gỡ: scripts/cleanup_today_pickleball.sql
-- =====================================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ---------- tham số ----------
SET @start = DATE('2026-11-03'), @end = DATE('2026-12-02'), @today = DATE('2026-11-14');
SET @days = 'TUE,THU,SAT', @dow = '3,5,7', @t1 = TIME('13:00'), @t2 = TIME('14:30');

-- ---------- dữ liệu nền ----------
SET @subject = (SELECT subject_id FROM subjects WHERE subject_name = 'Pickleball' AND status = 'ACTIVE' LIMIT 1);
SET @room    = (SELECT room_id FROM rooms WHERE room_name = 'Sân Pickleball' LIMIT 1);
SET @coach   = (SELECT user_id FROM users WHERE email = 'coach@Sportcenter.com');
SET @rec     = (SELECT user_id FROM users WHERE email = 'receptionist@Sportcenter.com');
SET @pkg     = (SELECT package_id FROM packages WHERE package_name = 'Pickleball 30 ngày' AND package_type = 'CLASS' LIMIT 1);
SET @price   = (SELECT price FROM packages WHERE package_id = @pkg);
SET @image   = (SELECT image_url FROM classes WHERE subject_id = @subject AND image_url IS NOT NULL ORDER BY class_id DESC LIMIT 1);

DROP TEMPORARY TABLE IF EXISTS _st;
CREATE TEMPORARY TABLE _st (idx INT PRIMARY KEY, email VARCHAR(100), method VARCHAR(20), member_id BIGINT NULL);
INSERT INTO _st (idx, email, method) VALUES
 (1, 'chinhnpse170133@fpt.edu.vn', 'CASH'),
 (2, 'nhanttse200464@gmail.com',   'BANK_TRANSFER'),
 (3, 'truongdu9805@gmail.com',     'CARD'),
 (4, 'chinh@gmail.com',            'CASH');
UPDATE _st s SET member_id = (SELECT u.user_id FROM users u WHERE u.email = s.email) WHERE s.idx BETWEEN 1 AND 4;

-- buổi học dự kiến
DROP TEMPORARY TABLE IF EXISTS _ps;
CREATE TEMPORARY TABLE _ps (st DATETIME PRIMARY KEY, en DATETIME);
INSERT INTO _ps
WITH RECURSIVE d AS (SELECT @start AS dt UNION ALL SELECT dt + INTERVAL 1 DAY FROM d WHERE dt < @end)
SELECT TIMESTAMP(d.dt, @t1), TIMESTAMP(d.dt, @t2) FROM d WHERE FIND_IN_SET(DAYOFWEEK(d.dt), @dow);

-- ---------- kiểm tra trước khi ghi ----------
-- MySQL không cho dùng 1 bảng tạm 2 lần trong 1 câu -> tách từng SET
SET @n_member = (SELECT COUNT(*) FROM _st WHERE member_id IS NOT NULL);
SET @base_ok  = @subject IS NOT NULL AND @room IS NOT NULL AND @coach IS NOT NULL AND @rec IS NOT NULL
                AND @pkg IS NOT NULL AND @n_member = 4;
SET @done     = EXISTS (SELECT 1 FROM classes WHERE subject_id = @subject AND start_date = @start AND start_clock = @t1);
SET @clash    = (SELECT COUNT(*) FROM _ps p JOIN class_sessions s
                 ON s.status <> 'CANCELLED' AND s.start_time < p.en AND s.end_time > p.st
                AND (s.coach_id = @coach OR s.room_id = @room));
-- học viên đã có lớp khác trùng giờ
SET @m_clash  = (SELECT COUNT(*) FROM _ps p
                 JOIN class_sessions s ON s.status <> 'CANCELLED' AND s.start_time < p.en AND s.end_time > p.st
                 JOIN class_registrations r ON r.class_id = s.class_id AND r.status = 'REGISTERED'
                 JOIN users u ON u.user_id = r.member_id
                 WHERE u.email IN ('chinhnpse170133@fpt.edu.vn', 'nhanttse200464@gmail.com', 'truongdu9805@gmail.com', 'chinh@gmail.com'));
SET @go = @base_ok AND NOT @done AND @clash = 0 AND @m_clash = 0;
SELECT CASE WHEN NOT @base_ok THEN CONCAT('DỪNG: thiếu dữ liệu nền (môn/phòng/HLV/lễ tân/gói Pickleball) hoặc chỉ tìm thấy ', @n_member, '/4 member')
            WHEN @done        THEN 'DỪNG: đã có lớp Pickleball 13:00 bắt đầu 03/11 (đã chạy rồi)'
            WHEN @clash > 0   THEN CONCAT('DỪNG: ', @clash, ' buổi trùng lịch HLV hoặc Sân Pickleball')
            WHEN @m_clash > 0 THEN CONCAT('DỪNG: ', @m_clash, ' buổi trùng với lớp khác của học viên')
            ELSE 'Bắt đầu thêm dữ liệu' END AS trang_thai;
-- Lưu ý: DB Aiven bật sql_require_primary_key -> mọi bảng tạm đều có PRIMARY KEY

-- ---------- lớp + buổi học ----------
INSERT INTO classes (days_of_week, duration_days, start_clock, end_clock, start_date, end_date, first_session_at,
                     start_time, end_time, max_capacity, status, image_url, coach_id, room_id, subject_id)
SELECT @days, 30, @t1, @t2, @start, @end, TIMESTAMP(@start, @t1),
       TIMESTAMP(@start, @t1), (SELECT MAX(en) FROM _ps), 12, 'OPENING', @image, @coach, @room, @subject
FROM DUAL WHERE @go;
SET @class = IF(@go, LAST_INSERT_ID(), NULL);
-- mã lớp giống SportClass.assignCodeIfMissing(): 3 chữ đầu tên môn + id 4 số
UPDATE classes SET class_code = CONCAT('PIC-', LPAD(class_id, 4, '0')) WHERE class_id = @class;

INSERT INTO class_sessions (start_time, end_time, status, type, coach_id, room_id, class_id)
SELECT p.st, p.en, IF(DATE(p.st) < @today, 'DONE', 'SCHEDULED'), 'REGULAR', @coach, @room, @class
FROM _ps p WHERE @go;

-- ---------- gói lớp + thanh toán + đăng ký ----------
-- mua ở quầy 1–3 ngày trước khai giảng
INSERT INTO subscriptions (paid_at, activated_at, start_date, end_date, status, receptionist_switch_count, member_id, package_id)
SELECT TIMESTAMP(@start - INTERVAL (1 + MOD(s.idx, 3)) DAY, '17:30') + INTERVAL (s.idx * 13) MINUTE,
       TIMESTAMP(@start, @t1), TIMESTAMP(@start, @t1), TIMESTAMP(@end, '00:00'), 'ACTIVE', 0, s.member_id, @pkg
FROM _st s WHERE @go;

DROP TEMPORARY TABLE IF EXISTS _sb;
CREATE TEMPORARY TABLE _sb (member_id BIGINT PRIMARY KEY, subscription_id BIGINT, paid_at DATETIME, method VARCHAR(20))
SELECT sb.member_id, sb.subscription_id, sb.paid_at, s.method
FROM subscriptions sb JOIN _st s ON s.member_id = sb.member_id
WHERE @go AND sb.package_id = @pkg AND sb.start_date = TIMESTAMP(@start, @t1);

INSERT INTO payments (amount, payment_method, payment_date, paid_at, status, transaction_ref, note, confirmed_by, subscription_id)
SELECT @price, b.method, b.paid_at, b.paid_at, 'COMPLETED',
       IF(b.method = 'BANK_TRANSFER', CONCAT('FT', DATE_FORMAT(b.paid_at, '%y%m%d'), LPAD(b.subscription_id, 6, '0')), NULL),
       'Thu tại quầy', @rec, b.subscription_id
FROM _sb b WHERE @go;

INSERT INTO class_registrations (registered_at, status, member_id, class_id, subscription_id)
SELECT b.paid_at + INTERVAL 3 MINUTE, 'REGISTERED', b.member_id, @class, b.subscription_id
FROM _sb b WHERE @go;

-- ---------- điểm danh 5 buổi đã qua (đa số có mặt, rải vài buổi vắng/muộn/có phép) ----------
INSERT INTO class_attendances (attendance_date, marked_at, status, note, session_id, member_id)
SELECT v.start_time, v.end_time + INTERVAL (4 + MOD(v.session_id, 7)) MINUTE,
       CASE v.h WHEN 0 THEN 'ABSENT' WHEN 1 THEN 'LATE' WHEN 2 THEN 'ON_LEAVE' ELSE 'PRESENT' END,
       CASE v.h WHEN 1 THEN 'Đến muộn khoảng 10 phút' WHEN 2 THEN 'Xin nghỉ có báo trước' ELSE NULL END,
       v.session_id, v.member_id
FROM (SELECT ss.session_id, ss.start_time, ss.end_time, b.member_id,
             MOD(b.member_id * 3 + ss.session_id, 7) AS h
      FROM class_sessions ss JOIN _sb b
      WHERE ss.class_id = @class AND ss.status = 'DONE') v
WHERE @go;

INSERT INTO check_ins (check_in_time, type, member_id, receptionist_id, class_session_id)
SELECT IF(a.status = 'LATE', ss.start_time + INTERVAL 10 MINUTE, ss.start_time - INTERVAL (5 + MOD(a.member_id, 10)) MINUTE),
       'CLASS', a.member_id, @rec, ss.session_id
FROM class_attendances a JOIN class_sessions ss ON ss.session_id = a.session_id
WHERE @go AND ss.class_id = @class AND a.status IN ('PRESENT', 'LATE');

-- ---------- tóm tắt ----------
SELECT k.class_id, k.class_code,
       (SELECT COUNT(*) FROM class_registrations r WHERE r.class_id = k.class_id) AS hoc_vien,
       (SELECT COUNT(*) FROM class_sessions s WHERE s.class_id = k.class_id AND s.status = 'DONE') AS buoi_da_hoc,
       (SELECT COUNT(*) FROM class_sessions s WHERE s.class_id = k.class_id) AS tong_buoi,
       (SELECT COUNT(*) FROM class_attendances a JOIN class_sessions s ON s.session_id = a.session_id WHERE s.class_id = k.class_id) AS luot_diem_danh,
       (SELECT s.session_id FROM class_sessions s WHERE s.class_id = k.class_id AND DATE(s.start_time) = @today) AS buoi_hom_nay
FROM classes k WHERE k.class_id = @class;

DROP TEMPORARY TABLE IF EXISTS _sb;
DROP TEMPORARY TABLE IF EXISTS _ps;
DROP TEMPORARY TABLE IF EXISTS _st;
COMMIT;
