-- Gỡ toàn bộ dữ liệu do scripts/seed_current.sql tạo: 29 học viên, 2 lớp Boxing/Yoga 13:00 từ 02/11/2026
-- và mọi thứ gắn với chúng. Không đụng tới dữ liệu khác. Chạy trong 1 transaction.
SET NAMES utf8mb4;
START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS _m;
-- DB Aiven bật sql_require_primary_key -> bảng tạm phải có PRIMARY KEY
CREATE TEMPORARY TABLE _m (PRIMARY KEY (user_id))
SELECT user_id FROM users WHERE email IN (
  'hoanglong.tran@gmail.com','tuankiet2001@gmail.com','thinhle.duc@gmail.com','minhquan.pham@gmail.com',
  'vothanhdat97@gmail.com','giahuy.huynh@gmail.com','dinhquocthai@gmail.com','maithanhha.0412@gmail.com',
  'tongochung@gmail.com','chaukhanhvy@gmail.com','haidang.lam@gmail.com','nhatminh.truong@gmail.com',
  'caothuylinh99@gmail.com','doanvanphat@gmail.com','kieuanhkhoa@gmail.com',
  'hongnhung.ng@gmail.com','myduyen.le@gmail.com','baongoc.pham@gmail.com','khanhhuyen.tran98@gmail.com',
  'vothanhtruc@gmail.com','minhthu.hoang@gmail.com','dangthuha.hn@gmail.com','buingocdiep@gmail.com',
  'yennhi.phan@gmail.com','haquynhnhu@gmail.com','luuhoangyen@gmail.com','taminhkhoi@gmail.com',
  'kimoanh.duong@gmail.com','ngoducanh.dn@gmail.com');

DROP TEMPORARY TABLE IF EXISTS _c;
CREATE TEMPORARY TABLE _c (PRIMARY KEY (class_id))
SELECT k.class_id FROM classes k
JOIN subjects s ON s.subject_id = k.subject_id
JOIN users u ON u.user_id = k.coach_id
WHERE (s.subject_name, k.start_date, k.start_clock, u.email) IN (
  ('Boxing', '2026-11-02', '13:00:00', 'coach.bao@seed.sportcenter.demo'),
  ('Yoga',   '2026-11-02', '13:00:00', 'coach.ha@seed.sportcenter.demo'));

DROP TEMPORARY TABLE IF EXISTS _p;
CREATE TEMPORARY TABLE _p (PRIMARY KEY (plan_id))
SELECT plan_id FROM class_plan WHERE class_id IN (SELECT class_id FROM _c)
UNION SELECT plan_id FROM member_plans WHERE member_id IN (SELECT user_id FROM _m);

-- thông báo gửi tới lớp / học viên sau khi tạo (nếu có): chỉ gỡ liên kết, giữ bản ghi thông báo
DELETE FROM notification_class   WHERE class_id IN (SELECT class_id FROM _c);
DELETE FROM notification_account WHERE user_id IN (SELECT user_id FROM _m);

DELETE FROM check_ins           WHERE member_id IN (SELECT user_id FROM _m);
DELETE FROM class_attendances   WHERE member_id IN (SELECT user_id FROM _m);
DELETE FROM class_registrations WHERE member_id IN (SELECT user_id FROM _m);
DELETE FROM workout_results     WHERE member_id IN (SELECT user_id FROM _m);
DELETE FROM member_plans        WHERE member_id IN (SELECT user_id FROM _m);
DELETE FROM class_plan          WHERE class_id IN (SELECT class_id FROM _c);
DELETE FROM workout_plans       WHERE plan_id IN (SELECT plan_id FROM _p)
  AND plan_id NOT IN (SELECT plan_id FROM class_plan) AND plan_id NOT IN (SELECT plan_id FROM member_plans);
DELETE p FROM payments p JOIN subscriptions s ON s.subscription_id = p.subscription_id
  WHERE s.member_id IN (SELECT user_id FROM _m);
DELETE FROM subscriptions       WHERE member_id IN (SELECT user_id FROM _m);
DELETE FROM class_sessions      WHERE class_id IN (SELECT class_id FROM _c);
DELETE FROM classes             WHERE class_id IN (SELECT class_id FROM _c);
DELETE FROM members             WHERE member_id IN (SELECT user_id FROM _m);
DELETE FROM users               WHERE user_id IN (SELECT user_id FROM _m);

SELECT (SELECT COUNT(*) FROM _m) AS hoc_vien_da_xoa, (SELECT COUNT(*) FROM _c) AS lop_da_xoa;
DROP TEMPORARY TABLE IF EXISTS _p;
DROP TEMPORARY TABLE IF EXISTS _c;
DROP TEMPORARY TABLE IF EXISTS _m;
COMMIT;
