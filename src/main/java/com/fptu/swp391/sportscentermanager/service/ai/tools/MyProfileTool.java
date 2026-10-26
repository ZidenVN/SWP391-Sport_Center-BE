package com.fptu.swp391.sportscentermanager.service.ai.tools;

import com.fptu.swp391.sportscentermanager.dto.MemberProfileResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.MemberService;
import com.fptu.swp391.sportscentermanager.service.ai.AiTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static com.fptu.swp391.sportscentermanager.service.ai.AiToolUtils.row;

@Component
@RequiredArgsConstructor
public class MyProfileTool implements AiTool {
    private final MemberService memberService;

    @Override
    public String name() {
        return "xem_ho_so_cua_toi";
    }

    @Override
    public String description() {
        return "Xem tên, giới tính và mục tiêu tập luyện của hội viên đang hỏi, dùng để gợi ý lớp hoặc gói phù hợp.";
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
        MemberProfileResponseDTO p = memberService.getProfile(user.getUser().getUserId());
        return row("ten", p.getFirstName(), "gioiTinh", p.getGender(), "mucTieu", p.getTrainingGoal());
    }
}
