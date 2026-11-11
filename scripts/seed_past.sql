-- =====================================================================================
-- Seed DỮ LIỆU QUÁ KHỨ cho demo (MySQL 8.0+). Chạy SAU scripts/seed_demo.py.
--
-- Tạo 3 lớp, mỗi lớp 15 học viên mới (past01..past45@seed.sportcenter.demo):
--   A. Yoga   T2-T4-T6 18:00-19:00  07/09 → 06/10/2026  (đã kết thúc)
--   B. Gym    T3-T5-T7 06:30-07:30  06/10 → 04/11/2026  (đã kết thúc)
--   C. Boxing T2-T4-T6 19:00-20:30  26/10 → 24/11/2026  (đang học)
-- Mỗi học viên: membership + gói lớp đã thanh toán đúng ngày, check-in kích hoạt, đăng ký lớp,
-- điểm danh mọi buổi đã qua, kết quả tập có nhận xét HLV; lớp có giáo án theo tuần; ~4 học viên/lớp có giáo án riêng.
--
-- An toàn: chỉ THÊM dữ liệu, chạy trong 1 transaction (lỗi -> ROLLBACK, DB giữ nguyên);
-- đã có past01 thì không làm gì (chạy lại không trùng). Mật khẩu học viên = mật khẩu của member01 (seed_demo).
-- Gỡ toàn bộ: scripts/cleanup_past.sql
-- =====================================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ---------- tra cứu dữ liệu nền (theo tên/email, không phụ thuộc ID) ----------
SET @domain  = 'seed.sportcenter.demo';
SET @go      = NOT EXISTS (SELECT 1 FROM users WHERE email = CONCAT('past01@', @domain));
SET @pw      = (SELECT password_hash FROM users WHERE email = CONCAT('member01@', @domain));
SET @role    = (SELECT role_id FROM roles WHERE role_name = 'MEMBER');
SET @rec     = (SELECT user_id FROM users WHERE email = CONCAT('reception.mai@', @domain));
-- thiếu dữ liệu nền (chưa chạy seed_demo.py) thì dừng hẳn bằng lỗi NOT NULL thay vì ghi dở
SET @go = IF(@pw IS NULL OR @role IS NULL OR @rec IS NULL, NULL, @go);
-- @go NULL hoặc 0 -> mọi câu INSERT/UPDATE bên dưới (đều có WHERE @go) không ghi gì
SELECT IF(@go IS NULL, 'THIẾU dữ liệu nền: chạy scripts/seed_demo.py trước',
          IF(@go, 'Bắt đầu seed dữ liệu quá khứ', 'Đã seed từ trước - bỏ qua')) AS trang_thai;
-- Lưu ý: DB Aiven bật sql_require_primary_key -> mọi bảng tạm bên dưới đều phải có PRIMARY KEY

-- ---------- 3 lớp ----------
DROP TEMPORARY TABLE IF EXISTS _cl;
CREATE TEMPORARY TABLE _cl (
  code CHAR(1) PRIMARY KEY, subject VARCHAR(50), days VARCHAR(20), dow VARCHAR(20),
  t1 TIME, t2 TIME, start DATE, coach_email VARCHAR(100), room VARCHAR(100), cap INT,
  class_pkg VARCHAR(100), class_id BIGINT NULL
);
INSERT INTO _cl (code, subject, days, dow, t1, t2, start, coach_email, room, cap, class_pkg) VALUES
 ('A', 'Yoga',   'MON,WED,FRI', '2,4,6', '18:00', '19:00', '2026-09-07', 'coach.ha@seed.sportcenter.demo',   'Phòng Yoga A',     15, 'Yoga Cân Bằng 30 ngày'),
 ('B', 'Gym',    'TUE,THU,SAT', '3,5,7', '06:30', '07:30', '2026-10-06', 'coach.tuan@seed.sportcenter.demo', 'Phòng Gym Tầng 2', 20, 'Gym Kèm HLV 30 ngày'),
 ('C', 'Boxing', 'MON,WED,FRI', '2,4,6', '19:00', '20:30', '2026-10-26', 'coach.bao@seed.sportcenter.demo',  'Sàn Boxing',       16, 'Fight Club Basic');

INSERT INTO classes (days_of_week, duration_days, start_clock, end_clock, start_date, end_date, first_session_at,
                     start_time, end_time, max_capacity, status, image_url, coach_id, room_id, subject_id)
SELECT c.days, 30, c.t1, c.t2, c.start, c.start + INTERVAL 29 DAY, TIMESTAMP(c.start, c.t1),
       TIMESTAMP(c.start, c.t1), TIMESTAMP(c.start + INTERVAL 29 DAY, c.t2), c.cap,
       IF(c.start + INTERVAL 29 DAY < CURDATE(), 'COMPLETED', 'OPENING'),
       (SELECT x.image_url FROM classes x WHERE x.subject_id = s.subject_id AND x.image_url IS NOT NULL ORDER BY x.class_id DESC LIMIT 1),
       u.user_id, r.room_id, s.subject_id
FROM _cl c
JOIN subjects s ON s.subject_name = c.subject AND s.status = 'ACTIVE'
JOIN users u    ON u.email = c.coach_email
JOIN rooms r    ON r.room_name = c.room
WHERE @go;

UPDATE _cl c SET class_id = (
  SELECT k.class_id FROM classes k JOIN subjects s ON s.subject_id = k.subject_id
  WHERE s.subject_name = c.subject AND k.start_date = c.start AND k.start_clock = c.t1
  ORDER BY k.class_id DESC LIMIT 1)
WHERE c.code IN ('A', 'B', 'C');  -- WHERE tường minh: DataGrip chặn UPDATE không có WHERE

-- buổi học theo lịch cố định; buổi đã qua = DONE, còn lại SCHEDULED
INSERT INTO class_sessions (start_time, end_time, status, type, coach_id, room_id, class_id)
WITH RECURSIVE d AS (SELECT DATE('2026-09-01') AS dt UNION ALL SELECT dt + INTERVAL 1 DAY FROM d WHERE dt < '2026-12-31')
SELECT TIMESTAMP(d.dt, c.t1), TIMESTAMP(d.dt, c.t2),
       IF(TIMESTAMP(d.dt, c.t2) < NOW(), 'DONE', 'SCHEDULED'), 'REGULAR', k.coach_id, k.room_id, k.class_id
FROM _cl c
JOIN classes k ON k.class_id = c.class_id
JOIN d ON d.dt BETWEEN c.start AND c.start + INTERVAL 29 DAY AND FIND_IN_SET(DAYOFWEEK(d.dt), c.dow)
WHERE @go;

UPDATE classes k
JOIN (SELECT class_id, MAX(end_time) AS last_end FROM class_sessions GROUP BY class_id) x ON x.class_id = k.class_id
SET k.end_time = x.last_end
WHERE @go AND k.class_id IN (SELECT class_id FROM _cl);

-- ---------- 45 học viên ----------
DROP TEMPORARY TABLE IF EXISTS _st;
CREATE TEMPORARY TABLE _st (
  idx INT PRIMARY KEY, code CHAR(1), ho VARCHAR(50), ten VARCHAR(50), gender VARCHAR(10),
  email VARCHAR(100), phone VARCHAR(20), goal VARCHAR(100), h DOUBLE, w DOUBLE,
  mship VARCHAR(100), method VARCHAR(20), personal TINYINT
);
INSERT INTO _st (idx, code, ho, ten, gender) VALUES
 (1,'A','Nguyễn','Thảo Vy','FEMALE'),(2,'A','Trần','Minh Châu','FEMALE'),(3,'A','Lê','Hải Yến','FEMALE'),
 (4,'A','Phạm','Quỳnh Anh','FEMALE'),(5,'A','Hoàng','Ngọc Hân','FEMALE'),(6,'A','Vũ','Thùy Dung','FEMALE'),
 (7,'A','Đặng','Bảo Trâm','FEMALE'),(8,'A','Bùi','Lan Chi','FEMALE'),(9,'A','Đỗ','Kim Ngân','FEMALE'),
 (10,'A','Hồ','Gia Bảo','MALE'),(11,'A','Ngô','Tường Vi','FEMALE'),(12,'A','Dương','Minh Tâm','MALE'),
 (13,'A','Lý','Phương Thảo','FEMALE'),(14,'A','Phan','Diệu Linh','FEMALE'),(15,'A','Trịnh','Hoài An','FEMALE'),
 (16,'B','Nguyễn','Quang Huy','MALE'),(17,'B','Trần','Đức Minh','MALE'),(18,'B','Lê','Hoàng Long','MALE'),
 (19,'B','Phạm','Tiến Đạt','MALE'),(20,'B','Hoàng','Văn Toàn','MALE'),(21,'B','Vũ','Nhật Nam','MALE'),
 (22,'B','Đặng','Thanh Tùng','MALE'),(23,'B','Bùi','Khánh Linh','FEMALE'),(24,'B','Đỗ','Mạnh Cường','MALE'),
 (25,'B','Hồ','Trung Hiếu','MALE'),(26,'B','Ngô','Bảo Anh','FEMALE'),(27,'B','Dương','Quốc Việt','MALE'),
 (28,'B','Lý','Thành Công','MALE'),(29,'B','Phan','Hữu Phước','MALE'),(30,'B','Trịnh','Ngọc Mai','FEMALE'),
 (31,'C','Nguyễn','Đăng Khoa','MALE'),(32,'C','Trần','Phúc Lâm','MALE'),(33,'C','Lê','Minh Nhật','MALE'),
 (34,'C','Phạm','Hà My','FEMALE'),(35,'C','Hoàng','Anh Tuấn','MALE'),(36,'C','Vũ','Quang Vinh','MALE'),
 (37,'C','Đặng','Thu Trang','FEMALE'),(38,'C','Bùi','Gia Khánh','MALE'),(39,'C','Đỗ','Hoàng Phúc','MALE'),
 (40,'C','Hồ','Ngọc Ánh','FEMALE'),(41,'C','Ngô','Đình Trọng','MALE'),(42,'C','Dương','Thanh Hằng','FEMALE'),
 (43,'C','Lý','Công Danh','MALE'),(44,'C','Phan','Bích Ngọc','FEMALE'),(45,'C','Trịnh','Văn Hậu','MALE');
UPDATE _st SET
  email    = CONCAT('past', LPAD(idx, 2, '0'), '@', @domain),
  phone    = CONCAT('098400', LPAD(idx, 4, '0')),
  goal     = ELT(MOD(idx, 5) + 1, 'Giảm cân, giảm mỡ', 'Tăng cơ, tăng sức mạnh', 'Dẻo dai, cải thiện tư thế', 'Giảm căng thẳng', 'Duy trì sức khỏe'),
  h        = IF(gender = 'MALE', 165 + MOD(idx * 7, 17), 152 + MOD(idx * 5, 15)),
  w        = IF(gender = 'MALE', 58 + MOD(idx * 11, 25), 45 + MOD(idx * 3, 18)),
  mship    = ELT(MOD(idx, 3) + 1, 'GÓI 1 THÁNG LINH HOẠT', 'GÓI 3 THÁNG TIẾT KIỆM', 'GÓI 6 THÁNG TOÀN HỆ THỐNG'),
  method   = ELT(MOD(idx, 4) + 1, 'CASH', 'CARD', 'BANK_TRANSFER', 'E_WALLET'),
  personal = (MOD(idx, 4) = 1)
WHERE idx BETWEEN 1 AND 45;

-- _st kèm thời điểm (bảng tạm không dùng 2 lần trong 1 câu được nên chép sẵn các mốc giờ)
DROP TEMPORARY TABLE IF EXISTS _sx;
CREATE TEMPORARY TABLE _sx (PRIMARY KEY (idx))
SELECT s.*, c.class_id, c.subject, c.start, c.t1, c.class_pkg,
       TIMESTAMP(c.start - INTERVAL (8 + MOD(s.idx, 6)) DAY, '08:00') + INTERVAL MOD(s.idx * 53, 600) MINUTE AS created,
       TIMESTAMP(c.start - INTERVAL (2 + MOD(s.idx, 4)) DAY, '09:00') + INTERVAL MOD(s.idx * 37, 480) MINUTE AS paid,
       TIMESTAMP(c.start, c.t1) - INTERVAL (20 + MOD(s.idx, 15)) MINUTE AS activated
FROM _st s JOIN _cl c ON c.code = s.code;

INSERT INTO users (create_at, email, first_name, last_name, gender, phone, password_hash, status, token_version, role_id)
SELECT x.created, x.email, x.ho, x.ten, x.gender, x.phone, @pw, 'ACTIVE', 0, @role
FROM _sx x WHERE @go;

INSERT INTO members (member_id, training_goal, height_cm, weight_kg, quiz_completed_at)
SELECT u.user_id, x.goal, x.h, x.w, x.created + INTERVAL 15 MINUTE
FROM _sx x JOIN users u ON u.email = x.email WHERE @go;

-- ---------- gói + thanh toán ----------
-- membership: kích hoạt ở lần check-in trước buổi đầu
INSERT INTO subscriptions (paid_at, activated_at, start_date, end_date, status, receptionist_switch_count, member_id, package_id)
SELECT x.paid, x.activated, x.activated, x.activated + INTERVAL p.duration_days DAY, 'ACTIVE', 0, u.user_id, p.package_id
FROM _sx x JOIN users u ON u.email = x.email
JOIN packages p ON p.package_name = x.mship
WHERE @go;

-- gói lớp: mua ngay sau membership, hiệu lực theo lớp
INSERT INTO subscriptions (paid_at, activated_at, start_date, end_date, status, receptionist_switch_count, member_id, package_id)
SELECT x.paid + INTERVAL 20 MINUTE, k.start_time, k.start_time, k.end_time, 'ACTIVE', 0, u.user_id, p.package_id
FROM _sx x JOIN users u ON u.email = x.email
JOIN packages p ON p.package_name = x.class_pkg
JOIN classes k ON k.class_id = x.class_id
WHERE @go;

INSERT INTO payments (amount, payment_method, payment_date, paid_at, status, transaction_ref, note, confirmed_by, subscription_id)
SELECT p.price, x.method, sb.paid_at, sb.paid_at, 'COMPLETED',
       IF(x.method = 'BANK_TRANSFER', CONCAT('FT', DATE_FORMAT(sb.paid_at, '%y%m%d'), LPAD(sb.subscription_id, 6, '0')), NULL),
       'Thu tại quầy', @rec, sb.subscription_id
FROM _sx x JOIN users u ON u.email = x.email
JOIN subscriptions sb ON sb.member_id = u.user_id
JOIN packages p ON p.package_id = sb.package_id
WHERE @go;

INSERT INTO class_registrations (registered_at, status, member_id, class_id, subscription_id)
SELECT sb.paid_at + INTERVAL 5 MINUTE, 'REGISTERED', u.user_id, x.class_id, sb.subscription_id
FROM _sx x JOIN users u ON u.email = x.email
JOIN subscriptions sb ON sb.member_id = u.user_id
JOIN packages p ON p.package_id = sb.package_id AND p.package_type = 'CLASS'
WHERE @go;

-- check-in kích hoạt membership
INSERT INTO check_ins (check_in_time, type, member_id, receptionist_id)
SELECT x.activated, 'FACILITY', u.user_id, @rec
FROM _sx x JOIN users u ON u.email = x.email WHERE @go;

-- ---------- điểm danh mọi buổi đã qua (~85% có mặt / muộn) ----------
INSERT INTO class_attendances (attendance_date, marked_at, status, note, session_id, member_id)
SELECT v.start_time, v.end_time + INTERVAL 5 MINUTE,
       CASE WHEN v.h < 2 THEN 'ABSENT' WHEN v.h = 2 THEN 'LATE' WHEN v.h = 3 THEN 'ON_LEAVE' ELSE 'PRESENT' END,
       CASE WHEN v.h = 2 THEN 'Đến muộn khoảng 10 phút' WHEN v.h = 3 THEN 'Xin nghỉ có báo trước' ELSE NULL END,
       v.session_id, v.member_id
FROM (SELECT ss.session_id, ss.start_time, ss.end_time, r.member_id,
             MOD(r.member_id * 7 + ss.session_id * 3, 20) AS h  -- ~80% có mặt, 10% vắng, 5% muộn, 5% nghỉ phép
      FROM class_sessions ss JOIN class_registrations r ON r.class_id = ss.class_id
      WHERE ss.status = 'DONE' AND ss.class_id IN (SELECT class_id FROM _cl)) v
WHERE @go;

-- check-in theo buổi cho người có mặt / đến muộn
INSERT INTO check_ins (check_in_time, type, member_id, receptionist_id, class_session_id)
SELECT IF(a.status = 'LATE', ss.start_time + INTERVAL 10 MINUTE, ss.start_time - INTERVAL 10 MINUTE), 'CLASS', a.member_id, @rec, ss.session_id
FROM class_attendances a JOIN class_sessions ss ON ss.session_id = a.session_id
WHERE @go AND a.status IN ('PRESENT', 'LATE') AND ss.class_id IN (SELECT class_id FROM _cl);

-- ---------- giáo án chung theo tuần ----------
DROP TEMPORARY TABLE IF EXISTS _wp;
CREATE TEMPORARY TABLE _wp (subject VARCHAR(50), wk INT, name VARCHAR(100), details TEXT, duration VARCHAR(50),
                            PRIMARY KEY (subject, wk));
INSERT INTO _wp VALUES
 ('Yoga', 1, 'Yoga nền tảng – Tuần 1', 'Khởi động khớp 10 phút; Chào mặt trời A x5; Tư thế chiến binh I, II giữ 30s mỗi bên; Tư thế cây 30s mỗi bên; Thở bụng 5 phút.', '60 phút'),
 ('Yoga', 2, 'Yoga mở hông – Tuần 2', 'Chào mặt trời A x5; Tư thế con bướm 1 phút; Tư thế bồ câu 45s mỗi bên; Gập người về trước 1 phút; Thư giãn nằm 5 phút.', '60 phút'),
 ('Yoga', 3, 'Yoga sức bền – Tuần 3', 'Chào mặt trời B x5; Plank 45s x3; Tư thế ghế 30s x3; Tư thế thuyền 30s x3; Vặn mình ngồi mỗi bên 30s.', '60 phút'),
 ('Yoga', 4, 'Yoga phục hồi – Tuần 4', 'Chuỗi chậm 20 phút; Tư thế em bé 2 phút; Gác chân lên tường 5 phút; Thiền thở 4-7-8 trong 10 phút.', '60 phút'),
 ('Gym', 1, 'Gym kỹ thuật cơ bản – Tuần 1', 'Squat tạ đòn 3x10 (tạ nhẹ, chỉnh kỹ thuật); Đẩy ngực máy 3x12; Kéo xô 3x12; Plank 3x30s; Đạp xe 10 phút.', '60 phút'),
 ('Gym', 2, 'Gym thân dưới – Tuần 2', 'Squat 4x8; Romanian deadlift 3x10; Lunge đi bộ 3x12 mỗi chân; Đẩy đùi máy 3x12; Bắp chân 3x15.', '60 phút'),
 ('Gym', 3, 'Gym thân trên – Tuần 3', 'Đẩy ngực tạ đòn 4x8; Chèo tạ đơn 3x10; Đẩy vai 3x10; Kéo xà hỗ trợ 3x8; Tay sau dây cáp 3x12.', '60 phút'),
 ('Gym', 4, 'Gym toàn thân – Tuần 4', 'Deadlift 4x6; Đẩy ngực nghiêng 3x10; Goblet squat 3x12; Farmer walk 3x30m; HIIT máy chạy 10 phút.', '60 phút'),
 ('Boxing', 1, 'Boxing thế đứng & jab – Tuần 1', 'Nhảy dây 3x2 phút; Thế đứng, di chuyển chân 10 phút; Jab - jab - cross vào bao 5x2 phút; Gập bụng 3x20.', '90 phút'),
 ('Boxing', 2, 'Boxing phối hợp đòn – Tuần 2', 'Nhảy dây 3x3 phút; Combo 1-2-3 trên lăm-pơ 6x2 phút; Né trái phải theo dây 3 hiệp; Burpee 3x10.', '90 phút'),
 ('Boxing', 3, 'Boxing phòng thủ – Tuần 3', 'Shadow boxing 3x3 phút; Đỡ - phản đòn với bạn tập 6x2 phút; Uppercut vào bao 4x2 phút; Plank 3x45s.', '90 phút'),
 ('Boxing', 4, 'Boxing sức bền – Tuần 4', 'Nhảy dây 5 phút; 8 hiệp bao x 3 phút nghỉ 1 phút; Sparring nhẹ có bảo hộ 3 hiệp; Giãn cơ 10 phút.', '90 phút');

INSERT INTO workout_plans (plan_name, exercise_details, duration, coach_id, create_at)
SELECT CONCAT(w.name, ' (lớp ', DATE_FORMAT(c.start, '%m/%Y'), ')'), w.details, w.duration, k.coach_id,
       TIMESTAMP(c.start + INTERVAL ((w.wk - 1) * 7 - 1) DAY, '20:00')
FROM _wp w JOIN _cl c ON c.subject = w.subject JOIN classes k ON k.class_id = c.class_id
WHERE @go AND TIMESTAMP(c.start + INTERVAL ((w.wk - 1) * 7 - 1) DAY, '20:00') <= NOW();

INSERT INTO class_plan (plan_id, class_id, assigned_date)
SELECT wp.plan_id, c.class_id, wp.create_at + INTERVAL 30 MINUTE
FROM _cl c JOIN classes k ON k.class_id = c.class_id
JOIN workout_plans wp ON wp.coach_id = k.coach_id AND wp.plan_name LIKE CONCAT('%(lớp ', DATE_FORMAT(c.start, '%m/%Y'), ')')
WHERE @go;

-- ---------- giáo án riêng cho ~4 học viên mỗi lớp ----------
INSERT INTO workout_plans (plan_name, exercise_details, duration, coach_id, create_at)
SELECT CONCAT('Giáo án riêng – ', x.ho, ' ', x.ten),
       CONCAT('Mục tiêu: ', x.goal, '. ',
              CASE x.subject
                WHEN 'Yoga' THEN 'Tự tập tại nhà 3 buổi/tuần: chào mặt trời x5, giữ tư thế cây và chiến binh II 45s mỗi bên, thở bụng 5 phút trước khi ngủ.'
                WHEN 'Gym'  THEN 'Thêm 2 buổi tự tập/tuần: squat 4x8 tăng 2,5kg mỗi tuần, chèo tạ đơn 3x10, đi bộ dốc 20 phút; ăn đủ 1,6g đạm/kg.'
                ELSE 'Thêm 2 buổi/tuần: nhảy dây 10 phút, shadow boxing 4x3 phút, chạy bộ 3km; giữ tay che cằm khi ra đòn.'
              END),
       '4 tuần', k.coach_id, x.start + INTERVAL 6 DAY + INTERVAL 21 HOUR
FROM _sx x JOIN classes k ON k.class_id = x.class_id
WHERE @go AND x.personal AND x.start + INTERVAL 6 DAY + INTERVAL 21 HOUR <= NOW();

INSERT INTO member_plans (plan_id, member_id, assigned_date, due_date, status)
SELECT wp.plan_id, u.user_id, DATE(x.start + INTERVAL 7 DAY), TIMESTAMP(x.start + INTERVAL 34 DAY, '23:59'),
       IF(x.start + INTERVAL 34 DAY < CURDATE(), 'COMPLETED', 'IN_PROGRESS')
FROM _sx x JOIN users u ON u.email = x.email
JOIN classes k ON k.class_id = x.class_id
JOIN workout_plans wp ON wp.coach_id = k.coach_id AND wp.plan_name = CONCAT('Giáo án riêng – ', x.ho, ' ', x.ten)
WHERE @go AND x.personal;

-- ---------- kết quả tập + nhận xét HLV (mỗi học viên tối đa 3 lần, chỉ những lần đã qua) ----------
DROP TEMPORARY TABLE IF EXISTS _k;
CREATE TEMPORARY TABLE _k (k INT PRIMARY KEY);
INSERT INTO _k VALUES (1), (2), (3);

INSERT INTO workout_results (activity_detail, coach_feedback, logged_date, member_id)
SELECT
  CASE x.subject
    WHEN 'Yoga' THEN CONCAT('Giữ plank ', 30 + q.k * 15 + MOD(x.idx, 4) * 5, 's; tư thế cây ', 20 + q.k * 10, 's mỗi bên; gập người chạm sàn cách ', GREATEST(0, 12 - q.k * 4 - MOD(x.idx, 3)), 'cm; cân nặng ', ROUND(x.w - q.k * 0.4, 1), 'kg.')
    WHEN 'Gym'  THEN CONCAT('Squat ', 30 + MOD(x.idx, 5) * 5 + q.k * 5, 'kg x 8 x 4 hiệp; đẩy ngực ', 25 + MOD(x.idx, 4) * 5 + q.k * 5, 'kg x 8 x 3; kéo xô ', 30 + q.k * 5, 'kg x 10 x 3; cân nặng ', ROUND(x.w + IF(x.goal LIKE 'Tăng cơ%', q.k * 0.5, -q.k * 0.5), 1), 'kg.')
    ELSE CONCAT('Nhảy dây ', 3 + q.k, ' hiệp x 2 phút không nghỉ; ', 4 + q.k * 2, ' hiệp bao x 2 phút; ', 70 + q.k * 15 + MOD(x.idx, 5) * 5, ' cú jab-cross/phút; nhịp tim nghỉ ', 82 - q.k * 3, ' bpm.')
  END,
  CASE x.subject
    WHEN 'Yoga' THEN ELT(q.k, 'Hơi thở còn gấp khi giữ tư thế, nhớ thở bằng bụng và hạ vai xuống.',
                              'Thăng bằng tiến bộ rõ, tuần sau thử tư thế cây nhắm mắt 10 giây.',
                              'Độ dẻo hông tăng nhiều so với buổi đầu, tiếp tục giãn cơ 10 phút mỗi tối.')
    WHEN 'Gym'  THEN ELT(q.k, 'Kỹ thuật squat ổn nhưng gối còn chụm vào trong, giữ gối theo hướng mũi chân.',
                              'Lên tạ đều, lưng giữ thẳng tốt. Ăn thêm bữa phụ giàu đạm sau buổi tập.',
                              'Hoàn thành tốt khóa, có thể chuyển sang chương trình 4 buổi/tuần.')
    ELSE ELT(q.k, 'Ra đòn còn hở cằm, tay sau phải luôn thủ trước mặt.',
                  'Phối hợp 1-2-3 nhanh hơn, cần xoay hông nhiều hơn khi tung cross.',
                  'Sức bền cải thiện rõ, đủ điều kiện tham gia sparring nhẹ có bảo hộ.')
  END,
  TIMESTAMP(x.start + INTERVAL (q.k * 9 - 2) DAY, x.t1) + INTERVAL 75 MINUTE,
  u.user_id
FROM _sx x JOIN users u ON u.email = x.email
JOIN _k q
WHERE @go AND TIMESTAMP(x.start + INTERVAL (q.k * 9 - 2) DAY, x.t1) + INTERVAL 75 MINUTE <= NOW();

-- ---------- tóm tắt ----------
SELECT c.code AS lop, c.subject AS mon, c.class_id,
       (SELECT COUNT(*) FROM class_registrations r WHERE r.class_id = c.class_id) AS hoc_vien,
       (SELECT COUNT(*) FROM class_sessions s WHERE s.class_id = c.class_id AND s.status = 'DONE') AS buoi_da_hoc,
       (SELECT COUNT(*) FROM class_sessions s WHERE s.class_id = c.class_id) AS tong_buoi,
       (SELECT COUNT(*) FROM class_attendances a JOIN class_sessions s ON s.session_id = a.session_id WHERE s.class_id = c.class_id) AS luot_diem_danh
FROM _cl c;

DROP TEMPORARY TABLE IF EXISTS _k;
DROP TEMPORARY TABLE IF EXISTS _wp;
DROP TEMPORARY TABLE IF EXISTS _sx;
DROP TEMPORARY TABLE IF EXISTS _st;
DROP TEMPORARY TABLE IF EXISTS _cl;
COMMIT;

