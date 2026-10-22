package com.fptu.swp391.sportscentermanager.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

// Trước đây status điểm danh bị lưu dạng số thứ tự ('0'..'3') và Hibernate tạo kèm CHECK (status between 0 and 3).
// Giờ lưu theo tên enum -> bỏ CHECK cũ và đổi các dòng còn dạng số. Chạy lại nhiều lần không ảnh hưởng.
@Component
@RequiredArgsConstructor
public class AttendanceStatusMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> ordinalChecks = jdbcTemplate.queryForList("""
                SELECT tc.CONSTRAINT_NAME
                FROM information_schema.TABLE_CONSTRAINTS tc
                JOIN information_schema.CHECK_CONSTRAINTS cc
                  ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                WHERE tc.TABLE_SCHEMA = DATABASE() AND tc.TABLE_NAME = 'class_attendances'
                  AND tc.CONSTRAINT_TYPE = 'CHECK' AND cc.CHECK_CLAUSE LIKE '%between 0 and 3%'
                """, String.class);
            for (String name : ordinalChecks) {
                jdbcTemplate.execute("ALTER TABLE class_attendances DROP CHECK `" + name + "`");
            }

            int fixed = jdbcTemplate.update("""
                UPDATE class_attendances
                SET status = CASE status WHEN '0' THEN 'PRESENT' WHEN '1' THEN 'ABSENT' WHEN '2' THEN 'LATE' WHEN '3' THEN 'ON_LEAVE' END
                WHERE status IN ('0', '1', '2', '3')
                """);
            if (fixed > 0 || !ordinalChecks.isEmpty()) {
                System.out.println("Đã chuyển trạng thái điểm danh sang dạng tên: " + fixed + " bản ghi");
            }
        } catch (Exception e) {
            // không chặn khởi động vì bước dọn dữ liệu cũ
            System.err.println("Bỏ qua chuyển đổi trạng thái điểm danh: " + e.getMessage());
        }
    }
}
