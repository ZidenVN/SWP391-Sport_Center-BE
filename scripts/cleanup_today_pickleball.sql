-- Gỡ dữ liệu của scripts/seed_today_pickleball.sql (lớp Pickleball T3-T5-T7 13:00 bắt đầu 03/11/2026).
-- Xoá cả những gì phát sinh khi demo trên lớp đó (điểm danh, check-in, thông báo lớp vẫn giữ).
SET NAMES utf8mb4;
START TRANSACTION;

SET @class = (SELECT k.class_id FROM classes k JOIN subjects s ON s.subject_id = k.subject_id
              WHERE s.subject_name = 'Pickleball' AND k.start_date = '2026-11-03' AND k.start_clock = '13:00:00'
              ORDER BY k.class_id DESC LIMIT 1);
SELECT IFNULL(CONCAT('Gỡ lớp #', @class), 'Không tìm thấy lớp, không xoá gì') AS trang_thai;

DROP TEMPORARY TABLE IF EXISTS _sub;
CREATE TEMPORARY TABLE _sub (subscription_id BIGINT PRIMARY KEY)
SELECT DISTINCT subscription_id FROM class_registrations WHERE class_id = @class AND subscription_id IS NOT NULL;

DELETE a FROM class_attendances a JOIN class_sessions s ON s.session_id = a.session_id WHERE s.class_id = @class;
DELETE c FROM check_ins c JOIN class_sessions s ON s.session_id = c.class_session_id WHERE s.class_id = @class;
DELETE FROM class_registrations WHERE class_id = @class;
DELETE p FROM payments p JOIN _sub x ON x.subscription_id = p.subscription_id;
DELETE sb FROM subscriptions sb JOIN _sub x ON x.subscription_id = sb.subscription_id;
DELETE FROM class_sessions WHERE class_id = @class;
DELETE FROM classes WHERE class_id = @class;

DROP TEMPORARY TABLE IF EXISTS _sub;
COMMIT;
