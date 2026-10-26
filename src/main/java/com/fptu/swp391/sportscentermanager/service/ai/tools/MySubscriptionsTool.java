package com.fptu.swp391.sportscentermanager.service.ai.tools;

import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import com.fptu.swp391.sportscentermanager.service.ai.AiTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static com.fptu.swp391.sportscentermanager.service.ai.AiToolUtils.row;

@Component
@RequiredArgsConstructor
public class MySubscriptionsTool implements AiTool {
    private final SubscriptionService subscriptionService;

    @Override
    public String name() {
        return "xem_goi_tap_cua_toi";
    }

    @Override
    public String description() {
        return "Xem các gói tập hội viên đang hỏi đã mua: tên gói, trạng thái, ngày bắt đầu, ngày hết hạn.";
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
        return subscriptionService.getSubscriptions(user.getUser().getUserId()).stream().map(s -> row(
            "tenGoi", s.getPackageName(),
            "trangThai", s.getStatus(),
            "batDau", s.getStartDate(),
            "hetHan", s.getEndDate()
        )).toList();
    }
}
