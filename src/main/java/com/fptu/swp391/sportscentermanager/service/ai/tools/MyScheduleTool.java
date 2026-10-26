package com.fptu.swp391.sportscentermanager.service.ai.tools;

import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.ClassRegistrationService;
import com.fptu.swp391.sportscentermanager.service.SportClassService;
import com.fptu.swp391.sportscentermanager.service.ai.AiTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static com.fptu.swp391.sportscentermanager.service.ai.AiToolUtils.row;

@Component
@RequiredArgsConstructor
public class MyScheduleTool implements AiTool {
    private final ClassRegistrationService classRegistrationService;

    @Override
    public String name() {
        return "xem_lich_tap_cua_toi";
    }

    @Override
    public String description() {
        return "Xem các lớp học sắp tới mà hội viên đang hỏi đã đăng ký.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of();
    }

    @Override
    public Set<String> roles() {
        return Set.of("MEMBER");
    }

    @Override
    public Object execute(Map<String, Object> args, CustomUserDetails user) {
        return classRegistrationService.getUpcomingSchedule(user.getUser().getUserId()).stream()
            .map(r -> row(
                "mon", r.getSubjectName(),
                "batDau", r.getStartTime(),
                "ketThuc", r.getEndTime(),
                "phong", r.getRoomName(),
                "hlv", r.getCoachName()))
            .toList();
    }
}
