package com.fptu.swp391.sportscentermanager.service.ai.tools;

import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.SportClassService;
import com.fptu.swp391.sportscentermanager.service.ai.AiTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static com.fptu.swp391.sportscentermanager.service.ai.AiToolUtils.row;

@Component
@RequiredArgsConstructor
public class UpcomingClassesTool implements AiTool {

    private final SportClassService sportClassService;

    @Override
    public String name() {
        return "xem_lop_sap_mo";
    }

    @Override
    public String description() {
        return "Xem các lớp học đang mở đăng ký và còn chỗ trống trong 14 ngày tới. Có thể lọc theo môn.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of("type", "object", "properties", Map.of("tenMon", Map.of("type", "string", "description", " Tên môn cần lọc, ví dụ Yoga. Bỏ trống để lấy tất cả.")));
    }

    @Override
    public Set<String> roles() {
        return Set.of("MEMBER");
    }

    @Override
    public Object execute(Map<String, Object> args, CustomUserDetails user) {
        String tenMon = args.get("tenMon") == null ? "" : args.get("tenMon").toString().toLowerCase();

        LocalDate today = LocalDate.now();
        return sportClassService.searchClasses(today, today.plusDays(14), null, null, null, "OPENING", true, 0, 20).getContent().stream().filter(c -> tenMon.isBlank() || (c.getSubjectName() != null && c.getSubjectName().toLowerCase().contains(tenMon))).map(c -> row("classId", c.getClassId(), "mon", c.getSubjectName(), "batDau", c.getStartTime(), "ketThuc", c.getEndTime(), "phong", c.getRoomName(), "hlv", c.getCoachName(), "conCho", c.getAvailableSeats())).toList();

    }
}
