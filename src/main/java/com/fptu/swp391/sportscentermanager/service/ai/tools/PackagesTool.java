package com.fptu.swp391.sportscentermanager.service.ai.tools;

import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.MembershipPackageService;
import com.fptu.swp391.sportscentermanager.service.ai.AiTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static com.fptu.swp391.sportscentermanager.service.ai.AiToolUtils.row;

@Component
@RequiredArgsConstructor
public class PackagesTool implements AiTool {
    private final MembershipPackageService membershipPackageService;

    public String name() { return "xem_goi_tap"; }
    public String description() { return "Xem danh sách gói tập đang bán của trung tâm: tên, giá, thời hạn, mô tả."; }
    public Map<String, Object> parameters() { return Map.of(); }
    public Set<String> roles() { return Set.of("MEMBER"); }

    public Object execute(Map<String, Object> args, CustomUserDetails user) {
        return membershipPackageService.getAllPackage().stream()
            .filter(p -> "ACTIVE".equals(p.getStatus()))
            .map(p -> row(
                "tenGoi", p.getPackageName(),
                "giaVnd", (long) p.getPrice(),
                "soNgay", p.getDurationDays(),
                "moTa", p.getDescription()))
            .toList();
    }
}
