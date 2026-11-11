-- =====================================================================================
-- Thêm 2 lớp ĐANG HỌC (MySQL 8.0+). Chạy SAU scripts/seed_demo.py.
--
--   B. Boxing T2-T4-T6 13:00-14:00  02/11 → 01/12/2026  HLV Lê Quốc Bảo  · Sàn Boxing   · 15 học viên
--   Y. Yoga   T2-T4-T6 13:00-14:00  02/11 → 01/12/2026  HLV Trần Thu Hà  · Phòng Yoga A · 14 học viên
-- Có buổi 13:00 hôm nay 11/11/2026 (chưa điểm danh). Các buổi 02, 04, 06, 09/11 đã học và đã điểm danh.
-- Mỗi học viên: membership + gói lớp thanh toán trước ngày khai giảng, check-in kích hoạt, đăng ký lớp,
-- điểm danh các buổi đã qua, check-in theo buổi; lớp có giáo án tuần 1–2; 3 học viên/lớp có giáo án riêng.
--
-- An toàn: chỉ THÊM dữ liệu, chạy trong 1 transaction. Dừng (không ghi gì) nếu:
--   thiếu dữ liệu nền, đã chạy rồi, email học viên đã tồn tại, hoặc HLV/phòng bị trùng lịch 13:00.
-- Mật khẩu học viên = mật khẩu của member01 (seed_demo). Gỡ: scripts/cleanup_current.sql
-- =====================================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ---------- dữ liệu nền ----------
SET @base = 'seed.sportcenter.demo';
SET @pw   = (SELECT password_hash FROM users WHERE email = CONCAT('member01@', @base));
SET @role = (SELECT role_id FROM roles WHERE role_name = 'MEMBER');
SET @rec  = (SELECT user_id FROM users WHERE email = CONCAT('reception.mai@', @base));

-- ---------- 2 lớp ----------
DROP TEMPORARY TABLE IF EXISTS _cl;
CREATE TEMPORARY TABLE _cl (
  code CHAR(1) PRIMARY KEY, subject VARCHAR(50), coach_email VARCHAR(100), room VARCHAR(100), cap INT,
  class_pkg VARCHAR(100), subject_id BIGINT NULL, coach_id BIGINT NULL, room_id BIGINT NULL, class_id BIGINT NULL
);
INSERT INTO _cl (code, subject, coach_email, room, cap, class_pkg) VALUES
 ('B', 'Boxing', 'coach.bao@seed.sportcenter.demo', 'Sàn Boxing',   16, 'Fight Club Basic'),
 ('Y', 'Yoga',   'coach.ha@seed.sportcenter.demo',  'Phòng Yoga A', 15, 'Yoga Cân Bằng 30 ngày');
SET @days = 'MON,WED,FRI', @dow = '2,4,6', @t1 = TIME('13:00'), @t2 = TIME('14:00'), @start = DATE('2026-11-02');

UPDATE _cl c SET
  subject_id = (SELECT s.subject_id FROM subjects s WHERE s.subject_name = c.subject AND s.status = 'ACTIVE' LIMIT 1),
  coach_id   = (SELECT u.user_id FROM users u WHERE u.email = c.coach_email),
  room_id    = (SELECT r.room_id FROM rooms r WHERE r.room_name = c.room LIMIT 1)
WHERE c.code IN ('B', 'Y');

-- buổi học dự kiến (để kiểm tra trùng lịch trước khi ghi)
DROP TEMPORARY TABLE IF EXISTS _ps;
CREATE TEMPORARY TABLE _ps (code CHAR(1), st DATETIME, en DATETIME, coach_id BIGINT, room_id BIGINT, PRIMARY KEY (code, st));
INSERT INTO _ps
WITH RECURSIVE d AS (SELECT @start AS dt UNION ALL SELECT dt + INTERVAL 1 DAY FROM d WHERE dt < @start + INTERVAL 29 DAY)
SELECT c.code, TIMESTAMP(d.dt, @t1), TIMESTAMP(d.dt, @t2), c.coach_id, c.room_id
FROM _cl c JOIN d ON FIND_IN_SET(DAYOFWEEK(d.dt), @dow);

SET @clash = (SELECT COUNT(*) FROM _ps p JOIN class_sessions s
              ON s.status <> 'CANCELLED' AND s.start_time < p.en AND s.end_time > p.st
             AND (s.coach_id = p.coach_id OR s.room_id = p.room_id));

-- ---------- 29 học viên ----------
DROP TEMPORARY TABLE IF EXISTS _st;
CREATE TEMPORARY TABLE _st (
  idx INT PRIMARY KEY, code CHAR(1), ho VARCHAR(50), ten VARCHAR(50), gender VARCHAR(10),
  email VARCHAR(100), phone VARCHAR(20), goal VARCHAR(100) NULL, h DOUBLE NULL, w DOUBLE NULL,
  mship VARCHAR(100) NULL, method VARCHAR(20) NULL, personal TINYINT DEFAULT 0
);
INSERT INTO _st (idx, code, ho, ten, gender, email, phone) VALUES
 ( 1,'B','Trần','Hoàng Long','MALE','hoanglong.tran@gmail.com','0903418275'),
 ( 2,'B','Nguyễn','Tuấn Kiệt','MALE','tuankiet2001@gmail.com','0912650384'),
 ( 3,'B','Lê','Đức Thịnh','MALE','thinhle.duc@gmail.com','0938271046'),
 ( 4,'B','Phạm','Minh Quân','MALE','minhquan.pham@gmail.com','0977304519'),
 ( 5,'B','Võ','Thành Đạt','MALE','vothanhdat97@gmail.com','0868142903'),
 ( 6,'B','Huỳnh','Gia Huy','MALE','giahuy.huynh@gmail.com','0335906217'),
 ( 7,'B','Đinh','Quốc Thái','MALE','dinhquocthai@gmail.com','0702583164'),
 ( 8,'B','Mai','Thanh Hà','FEMALE','maithanhha.0412@gmail.com','0899217630'),
 ( 9,'B','Tô','Ngọc Hưng','MALE','tongochung@gmail.com','0985063728'),
 (10,'B','Châu','Khánh Vy','FEMALE','chaukhanhvy@gmail.com','0367419052'),
 (11,'B','Lâm','Hải Đăng','MALE','haidang.lam@gmail.com','0914738206'),
 (12,'B','Trương','Nhật Minh','MALE','nhatminh.truong@gmail.com','0933580471'),
 (13,'B','Cao','Thùy Linh','FEMALE','caothuylinh99@gmail.com','0796325814'),
 (14,'B','Đoàn','Văn Phát','MALE','doanvanphat@gmail.com','0378640192'),
 (15,'B','Kiều','Anh Khoa','MALE','kieuanhkhoa@gmail.com','0905172938'),
 (16,'Y','Nguyễn','Hồng Nhung','FEMALE','hongnhung.ng@gmail.com','0909847153'),
 (17,'Y','Lê','Mỹ Duyên','FEMALE','myduyen.le@gmail.com','0947215806'),
 (18,'Y','Phạm','Bảo Ngọc','FEMALE','baongoc.pham@gmail.com','0328719460'),
 (19,'Y','Trần','Khánh Huyền','FEMALE','khanhhuyen.tran98@gmail.com','0961054387'),
 (20,'Y','Võ','Thanh Trúc','FEMALE','vothanhtruc@gmail.com','0786430925'),
 (21,'Y','Hoàng','Minh Thư','FEMALE','minhthu.hoang@gmail.com','0918362047'),
 (22,'Y','Đặng','Thu Hà','FEMALE','dangthuha.hn@gmail.com','0352098716'),
 (23,'Y','Bùi','Ngọc Diệp','FEMALE','buingocdiep@gmail.com','0886715240'),
 (24,'Y','Phan','Yến Nhi','FEMALE','yennhi.phan@gmail.com','0937406158'),
 (25,'Y','Hà','Quỳnh Như','FEMALE','haquynhnhu@gmail.com','0705291836'),
 (26,'Y','Lưu','Hoàng Yến','FEMALE','luuhoangyen@gmail.com','0976183402'),
 (27,'Y','Tạ','Minh Khôi','MALE','taminhkhoi@gmail.com','0394527061'),
 (28,'Y','Dương','Kim Oanh','FEMALE','kimoanh.duong@gmail.com','0908634271'),
 (29,'Y','Ngô','Đức Anh','MALE','ngoducanh.dn@gmail.com','0829471563');
UPDATE _st SET
  goal = IF(code = 'B',
            ELT(MOD(idx, 4) + 1, 'Giảm cân, giảm mỡ', 'Tăng cơ, tăng sức mạnh', 'Tự vệ, tăng phản xạ', 'Tăng sức bền'),
            ELT(MOD(idx, 4) + 1, 'Dẻo dai, cải thiện tư thế', 'Giảm căng thẳng', 'Giảm đau lưng, mỏi cổ vai', 'Duy trì sức khỏe')),
  h    = IF(gender = 'MALE', 164 + MOD(idx * 7, 18), 151 + MOD(idx * 5, 15)),
  w    = IF(gender = 'MALE', 57 + MOD(idx * 11, 26), 44 + MOD(idx * 3, 17)),
  mship  = ELT(MOD(idx, 3) + 1, 'GÓI 1 THÁNG LINH HOẠT', 'GÓI 3 THÁNG TIẾT KIỆM', 'GÓI 6 THÁNG TOÀN HỆ THỐNG'),
  method = ELT(MOD(idx, 4) + 1, 'CASH', 'CARD', 'BANK_TRANSFER', 'E_WALLET'),
  personal = idx IN (1, 4, 10, 16, 20, 25)
WHERE idx BETWEEN 1 AND 29;

SET @taken = (SELECT COUNT(*) FROM users u JOIN _st s ON s.email = u.email);
SET @done  = EXISTS (SELECT 1 FROM classes k JOIN _cl c ON c.subject_id = k.subject_id
                     WHERE k.start_date = @start AND k.start_clock = @t1);
-- MySQL không cho dùng 1 bảng tạm 2 lần trong 1 câu -> tách từng SET
SET @refs_ok = (SELECT COUNT(*) FROM _cl WHERE subject_id IS NULL OR coach_id IS NULL OR room_id IS NULL) = 0;
SET @pkg_ok  = (SELECT COUNT(DISTINCT c.code) FROM _cl c JOIN packages p ON p.package_name = c.class_pkg) = 2;
SET @base_ok = @pw IS NOT NULL AND @role IS NOT NULL AND @rec IS NOT NULL AND @refs_ok AND @pkg_ok;
SET @go = @base_ok AND NOT @done AND @taken = 0 AND @clash = 0;
SELECT CASE WHEN NOT @base_ok THEN 'DỪNG: thiếu dữ liệu nền (môn, phòng, HLV, gói, member01, lễ tân)'
            WHEN @done        THEN 'DỪNG: đã có lớp Boxing/Yoga 13:00 bắt đầu 02/11 (đã chạy rồi)'
            WHEN @taken > 0   THEN CONCAT('DỪNG: ', @taken, ' email học viên đã tồn tại')
            WHEN @clash > 0   THEN CONCAT('DỪNG: ', @clash, ' buổi trùng lịch HLV hoặc phòng lúc 13:00')
            ELSE 'Bắt đầu thêm dữ liệu' END AS trang_thai;
-- Lưu ý: DB Aiven bật sql_require_primary_key -> mọi bảng tạm đều có PRIMARY KEY

INSERT INTO classes (days_of_week, duration_days, start_clock, end_clock, start_date, end_date, first_session_at,
                     start_time, end_time, max_capacity, status, image_url, coach_id, room_id, subject_id)
SELECT @days, 30, @t1, @t2, @start, @start + INTERVAL 29 DAY, TIMESTAMP(@start, @t1),
       TIMESTAMP(@start, @t1), TIMESTAMP(@start + INTERVAL 29 DAY, @t2), c.cap, 'OPENING',
       (SELECT x.image_url FROM classes x WHERE x.subject_id = c.subject_id AND x.image_url IS NOT NULL ORDER BY x.class_id DESC LIMIT 1),
       c.coach_id, c.room_id, c.subject_id
FROM _cl c WHERE @go;

UPDATE _cl c SET class_id = (
  SELECT k.class_id FROM classes k
  WHERE k.subject_id = c.subject_id AND k.start_date = @start AND k.start_clock = @t1
  ORDER BY k.class_id DESC LIMIT 1)
WHERE c.code IN ('B', 'Y');  -- WHERE tường minh: DataGrip chặn UPDATE không có WHERE

INSERT INTO class_sessions (start_time, end_time, status, type, coach_id, room_id, class_id)
SELECT p.st, p.en, IF(p.en < NOW(), 'DONE', 'SCHEDULED'), 'REGULAR', p.coach_id, p.room_id, c.class_id
FROM _ps p JOIN _cl c ON c.code = p.code
WHERE @go;

UPDATE classes k
JOIN (SELECT class_id, MAX(end_time) AS last_end FROM class_sessions GROUP BY class_id) x ON x.class_id = k.class_id
SET k.end_time = x.last_end
WHERE @go AND k.class_id IN (SELECT class_id FROM _cl);

-- học viên kèm các mốc thời gian (bảng tạm không dùng 2 lần trong 1 câu được nên chép sẵn)
DROP TEMPORARY TABLE IF EXISTS _sx;
CREATE TEMPORARY TABLE _sx (PRIMARY KEY (idx))
SELECT s.*, c.class_id, c.subject, c.class_pkg,
       TIMESTAMP(@start - INTERVAL (9 + MOD(s.idx * 5, 12)) DAY, '08:00') + INTERVAL MOD(s.idx * 53, 720) MINUTE AS created,
       TIMESTAMP(@start - INTERVAL (2 + MOD(s.idx, 5)) DAY, '09:00') + INTERVAL MOD(s.idx * 37, 540) MINUTE AS paid,
       TIMESTAMP(@start, @t1) - INTERVAL (15 + MOD(s.idx * 3, 20)) MINUTE AS activated
FROM _st s JOIN _cl c ON c.code = s.code;

INSERT INTO users (create_at, email, first_name, last_name, gender, phone, password_hash, status, token_version, role_id)
SELECT x.created, x.email, x.ho, x.ten, x.gender, x.phone, @pw, 'ACTIVE', 0, @role
FROM _sx x WHERE @go;

INSERT INTO members (member_id, training_goal, height_cm, weight_kg, quiz_completed_at)
SELECT u.user_id, x.goal, x.h, x.w, x.created + INTERVAL 12 MINUTE
FROM _sx x JOIN users u ON u.email = x.email WHERE @go;

-- ---------- gói + thanh toán ----------
INSERT INTO subscriptions (paid_at, activated_at, start_date, end_date, status, receptionist_switch_count, member_id, package_id)
SELECT x.paid, x.activated, x.activated, x.activated + INTERVAL p.duration_days DAY, 'ACTIVE', 0, u.user_id, p.package_id
FROM _sx x JOIN users u ON u.email = x.email
JOIN packages p ON p.package_name = x.mship
WHERE @go;

INSERT INTO subscriptions (paid_at, activated_at, start_date, end_date, status, receptionist_switch_count, member_id, package_id)
SELECT x.paid + INTERVAL (8 + MOD(x.idx, 15)) MINUTE, k.start_time, k.start_time, k.end_time, 'ACTIVE', 0, u.user_id, p.package_id
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
SELECT sb.paid_at + INTERVAL 4 MINUTE, 'REGISTERED', u.user_id, x.class_id, sb.subscription_id
FROM _sx x JOIN users u ON u.email = x.email
JOIN subscriptions sb ON sb.member_id = u.user_id
JOIN packages p ON p.package_id = sb.package_id AND p.package_type = 'CLASS'
WHERE @go;

-- check-in lần đầu (kích hoạt membership) ngay trước buổi khai giảng
INSERT INTO check_ins (check_in_time, type, member_id, receptionist_id)
SELECT x.activated, 'FACILITY', u.user_id, @rec
FROM _sx x JOIN users u ON u.email = x.email WHERE @go;

-- ---------- điểm danh các buổi đã qua (~80% có mặt, 10% vắng, 5% muộn, 5% có phép) ----------
INSERT INTO class_attendances (attendance_date, marked_at, status, note, session_id, member_id)
SELECT v.start_time, v.end_time + INTERVAL (3 + MOD(v.session_id, 9)) MINUTE,
       CASE WHEN v.h < 2 THEN 'ABSENT' WHEN v.h = 2 THEN 'LATE' WHEN v.h = 3 THEN 'ON_LEAVE' ELSE 'PRESENT' END,
       CASE WHEN v.h = 2 THEN ELT(MOD(v.member_id, 3) + 1, 'Đến muộn khoảng 10 phút', 'Kẹt xe, vào lớp lúc 13:15', 'Đến muộn, bỏ phần khởi động')
            WHEN v.h = 3 THEN ELT(MOD(v.member_id, 3) + 1, 'Xin nghỉ có báo trước', 'Nghỉ ốm, đã nhắn HLV', 'Bận việc gia đình, báo trước 1 ngày')
            ELSE NULL END,
       v.session_id, v.member_id
FROM (SELECT ss.session_id, ss.start_time, ss.end_time, r.member_id,
             MOD(r.member_id * 7 + ss.session_id * 3, 20) AS h
      FROM class_sessions ss JOIN class_registrations r ON r.class_id = ss.class_id
      WHERE ss.status = 'DONE' AND ss.class_id IN (SELECT class_id FROM _cl)) v
WHERE @go;

INSERT INTO check_ins (check_in_time, type, member_id, receptionist_id, class_session_id)
SELECT IF(a.status = 'LATE', ss.start_time + INTERVAL 12 MINUTE, ss.start_time - INTERVAL (5 + MOD(a.member_id, 12)) MINUTE),
       'CLASS', a.member_id, @rec, ss.session_id
FROM class_attendances a JOIN class_sessions ss ON ss.session_id = a.session_id
WHERE @go AND a.status IN ('PRESENT', 'LATE') AND ss.class_id IN (SELECT class_id FROM _cl);

-- ---------- giáo án chung tuần 1–2 ----------
DROP TEMPORARY TABLE IF EXISTS _wp;
CREATE TEMPORARY TABLE _wp (subject VARCHAR(50), wk INT, name VARCHAR(150), details TEXT, PRIMARY KEY (subject, wk));
INSERT INTO _wp VALUES
 ('Boxing', 1, 'Boxing căn bản: thế thủ và jab', 'Khởi động khớp, nhảy dây 3x2 phút; Thế thủ, bước chân tiến lùi 10 phút; Jab và jab-cross vào bao 4x2 phút; Gập bụng 3x15; Giãn cơ 5 phút.'),
 ('Boxing', 2, 'Boxing: combo 1-2 và né đòn', 'Nhảy dây 3x2 phút; Combo 1-2 trên lăm-pơ 5x2 phút; Né đầu trái phải theo dây 3 hiệp; Burpee 3x8; Giãn cơ vai, hông 5 phút.'),
 ('Yoga',   1, 'Yoga khởi đầu: hơi thở và tư thế đứng', 'Thở bụng 5 phút; Chào mặt trời A x3; Tư thế núi, chiến binh I giữ 30s mỗi bên; Tư thế cây 20s mỗi bên; Thư giãn nằm 5 phút.'),
 ('Yoga',   2, 'Yoga giãn cơ lưng và vai', 'Mèo - bò 1 phút; Chào mặt trời A x4; Rắn hổ mang 3x20s; Gập người về trước 1 phút; Vặn mình nằm mỗi bên 45s; Thư giãn 5 phút.');

INSERT INTO workout_plans (plan_name, exercise_details, duration, coach_id, create_at)
SELECT CONCAT(w.name, ' – Tuần ', w.wk, ' (lớp T2-T4-T6 13:00)'), w.details, '60 phút', c.coach_id,
       TIMESTAMP(@start + INTERVAL ((w.wk - 1) * 7 - 1) DAY, '20:30')
FROM _wp w JOIN _cl c ON c.subject = w.subject
WHERE @go AND TIMESTAMP(@start + INTERVAL ((w.wk - 1) * 7 - 1) DAY, '20:30') <= NOW();

INSERT INTO class_plan (plan_id, class_id, assigned_date)
SELECT wp.plan_id, c.class_id, wp.create_at + INTERVAL 20 MINUTE
FROM _cl c JOIN workout_plans wp ON wp.coach_id = c.coach_id AND wp.plan_name LIKE '%(lớp T2-T4-T6 13:00)'
WHERE @go;

-- ---------- giáo án riêng cho 3 học viên mỗi lớp ----------
INSERT INTO workout_plans (plan_name, exercise_details, duration, coach_id, create_at)
SELECT CONCAT('Giáo án riêng – ', x.ho, ' ', x.ten),
       CONCAT('Mục tiêu: ', x.goal, '. ',
              IF(x.subject = 'Boxing',
                 'Tự tập thêm 2 buổi/tuần: nhảy dây 10 phút, shadow boxing 3x3 phút giữ tay che cằm, chạy bộ 2–3km.',
                 'Tự tập tại nhà 3 buổi/tuần: chào mặt trời x4, mèo - bò 1 phút, giữ tư thế cây 30s mỗi bên, thở bụng 5 phút trước khi ngủ.')),
       '3 tuần', k.coach_id, TIMESTAMP(@start + INTERVAL 4 DAY, '21:00')
FROM _sx x JOIN classes k ON k.class_id = x.class_id
WHERE @go AND x.personal;

INSERT INTO member_plans (plan_id, member_id, assigned_date, due_date, status)
SELECT wp.plan_id, u.user_id, @start + INTERVAL 4 DAY, TIMESTAMP(@start + INTERVAL 29 DAY, '23:59'), 'IN_PROGRESS'
FROM _sx x JOIN users u ON u.email = x.email
JOIN classes k ON k.class_id = x.class_id
JOIN workout_plans wp ON wp.coach_id = k.coach_id AND wp.plan_name = CONCAT('Giáo án riêng – ', x.ho, ' ', x.ten)
WHERE @go AND x.personal;

-- ---------- kết quả tập + nhận xét HLV (buổi 04/11 và 09/11) ----------
DROP TEMPORARY TABLE IF EXISTS _k;
CREATE TEMPORARY TABLE _k (k INT PRIMARY KEY, dd INT);
INSERT INTO _k VALUES (1, 2), (2, 7);

INSERT INTO workout_results (activity_detail, coach_feedback, logged_date, member_id)
SELECT
  IF(x.subject = 'Boxing',
     CONCAT('Nhảy dây ', 2 + q.k, ' hiệp x 2 phút; ', 3 + q.k * 2, ' hiệp bao x 2 phút; ', 55 + q.k * 12 + MOD(x.idx, 5) * 4,
            ' cú jab-cross/phút; nhịp tim nghỉ ', 86 - q.k * 3 - MOD(x.idx, 4), ' bpm.'),
     CONCAT('Giữ plank ', 25 + q.k * 10 + MOD(x.idx, 4) * 5, 's; tư thế cây ', 15 + q.k * 10, 's mỗi bên; gập người chạm sàn cách ',
            GREATEST(0, 15 - q.k * 4 - MOD(x.idx, 3)), 'cm.')),
  IF(x.subject = 'Boxing',
     ELT(q.k, ELT(MOD(x.idx, 3) + 1, 'Ra đòn còn hở cằm, tay sau phải luôn thủ trước mặt.',
                                     'Chân còn đứng thẳng hàng, giữ thế lệch để dễ di chuyển.',
                                     'Jab nhanh nhưng thu tay chậm, tập thu tay về ngay sau khi ra đòn.'),
              ELT(MOD(x.idx, 3) + 1, 'Thủ cằm tốt hơn buổi đầu, cần xoay hông khi tung cross.',
                                     'Di chuyển chân đã ổn, tiếp tục giữ nhịp thở khi ra combo.',
                                     'Sức bền tăng rõ, hoàn thành đủ hiệp bao không nghỉ.')),
     ELT(q.k, ELT(MOD(x.idx, 3) + 1, 'Hơi thở còn gấp khi giữ tư thế, nhớ thở bằng bụng và hạ vai.',
                                     'Lưng còn cong khi gập người, ưu tiên giữ lưng thẳng hơn là chạm sàn.',
                                     'Thăng bằng tốt, chú ý không khóa gối khi đứng tư thế cây.'),
              ELT(MOD(x.idx, 3) + 1, 'Kiểm soát hơi thở tốt hơn, giữ tư thế lâu hơn tuần trước.',
                                     'Độ dẻo lưng tiến bộ, tiếp tục giãn cơ 10 phút mỗi tối.',
                                     'Tư thế cây vững hơn, tuần sau thử giữ 45 giây.'))),
  TIMESTAMP(@start + INTERVAL q.dd DAY, @t2) + INTERVAL (10 + MOD(x.idx * 7, 40)) MINUTE,
  u.user_id
FROM _sx x JOIN users u ON u.email = x.email
JOIN _k q
WHERE @go AND TIMESTAMP(@start + INTERVAL q.dd DAY, @t2) <= NOW();

-- ---------- tóm tắt ----------
SELECT c.subject AS mon, c.class_id,
       (SELECT COUNT(*) FROM class_registrations r WHERE r.class_id = c.class_id) AS hoc_vien,
       (SELECT COUNT(*) FROM class_sessions s WHERE s.class_id = c.class_id AND s.status = 'DONE') AS buoi_da_hoc,
       (SELECT COUNT(*) FROM class_sessions s WHERE s.class_id = c.class_id) AS tong_buoi,
       (SELECT COUNT(*) FROM class_attendances a JOIN class_sessions s ON s.session_id = a.session_id WHERE s.class_id = c.class_id) AS luot_diem_danh
FROM _cl c;

-- điểm danh từng học viên (để chọn tài khoản trình bày)
SELECT x.subject AS mon, x.email, CONCAT(x.ho, ' ', x.ten) AS ho_ten, IF(x.personal, 'có', '') AS giao_an_rieng,
       SUM(a.status = 'PRESENT') AS co_mat, SUM(a.status = 'LATE') AS muon,
       SUM(a.status = 'ABSENT') AS vang, SUM(a.status = 'ON_LEAVE') AS co_phep
FROM _sx x JOIN users u ON u.email = x.email
LEFT JOIN class_attendances a ON a.member_id = u.user_id
GROUP BY x.idx, x.subject, x.email, x.ho, x.ten, x.personal
ORDER BY x.idx;

DROP TEMPORARY TABLE IF EXISTS _k;
DROP TEMPORARY TABLE IF EXISTS _wp;
DROP TEMPORARY TABLE IF EXISTS _sx;
DROP TEMPORARY TABLE IF EXISTS _st;
DROP TEMPORARY TABLE IF EXISTS _ps;
DROP TEMPORARY TABLE IF EXISTS _cl;
COMMIT;
