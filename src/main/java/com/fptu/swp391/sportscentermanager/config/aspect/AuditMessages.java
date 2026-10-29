package com.fptu.swp391.sportscentermanager.config.aspect;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Mẫu câu audit log theo "actionType:targetEntity". [ ... ] = đoạn tuỳ chọn, bỏ đi khi thiếu giá trị bên trong
final class AuditMessages {
    private AuditMessages() {}

    static final Map<String, String> ROLES = Map.of(
        "CENTER_MANAGER", "Quản lý", "RECEPTIONIST", "Lễ tân", "COACH", "HLV", "MEMBER", "Hội viên");

    private static final Map<String, String> TEMPLATES = Map.ofEntries(
        Map.entry("REGISTER_ACCOUNT:User", "đã đăng ký tài khoản[ {name}]"),
        Map.entry("SUBSCRIBE_PACKAGE:Member", "đã đăng ký gói[ \"{package}\"][ cho hội viên {member}]"),
        Map.entry("CONFIRM_PAYMENT:Payment", "đã xác nhận thanh toán[ gói \"{package}\"][ của hội viên {member}]"),
        Map.entry("CANCEL_PAYMENT:Payment", "đã huỷ giao dịch[ gói \"{package}\"][ của hội viên {member}]"),
        Map.entry("REFUND_PAYMENT:Payment", "đã hoàn tiền[ gói \"{package}\"][ cho hội viên {member}]"),
        Map.entry("CHECK_IN:Member", "đã check-in cho hội viên[ {member}][ (gói \"{package}\")]"),

        Map.entry("CREATE:SportClass", "đã mở lớp[ {subject}][ lúc {time}]"),
        Map.entry("UPDATE:SportClass", "đã cập nhật lớp[ {subject}][ lúc {time}]"),
        Map.entry("CANCEL:SportClass", "đã huỷ lớp[ {subject}][ lúc {time}]"),
        Map.entry("COMPLETE:SportClass", "đã đánh dấu hoàn thành lớp[ {subject}][ lúc {time}]"),
        Map.entry("DELETE:SportClass", "đã xoá lớp[ #{id}]"),
        Map.entry("ASSIGN_COACH:SportClass", "đã phân công HLV[ {coach}] cho lớp[ {subject}][ lúc {time}]"),

        Map.entry("CREATE:Coach", "đã tạo tài khoản HLV[ {name}]"),
        Map.entry("UPDATE:Coach", "đã cập nhật thông tin HLV[ {name}]"),
        Map.entry("DELETE:Coach", "đã ngừng hoạt động HLV[ {name}]"),
        Map.entry("CREATE:Receptionist", "đã tạo tài khoản lễ tân[ {name}]"),
        Map.entry("UPDATE:Receptionist", "đã cập nhật thông tin lễ tân[ {name}]"),
        Map.entry("TOGGLE_STATUS:Receptionist", "đã {toggle} tài khoản lễ tân[ {name}]"),
        Map.entry("CREATE:Member", "đã tạo tài khoản hội viên[ {name}]"),
        Map.entry("UPDATE:Member", "đã cập nhật thông tin hội viên[ {name}]"),
        Map.entry("UPDATE_PROFILE:Member", "đã cập nhật hồ sơ cá nhân"),
        Map.entry("UPDATE:User", "đã cập nhật tài khoản[ {name}]"),
        Map.entry("TOGGLE_STATUS:User", "đã {toggle} tài khoản[ {name}]"),
        Map.entry("ASSIGN_ROLE:User", "đã đổi vai trò tài khoản[ {name}][ thành {role}]"),
        Map.entry("UPDATE_PERMISSIONS:Role", "đã cập nhật quyền của vai trò[ #{id}]"),

        Map.entry("CREATE:MembershipPackage", "đã tạo gói tập[ \"{package}\"]"),
        Map.entry("UPDATE:MembershipPackage", "đã cập nhật gói tập[ \"{package}\"]"),
        Map.entry("DELETE:MembershipPackage", "đã ngừng bán gói tập[ #{id}]"),
        Map.entry("CREATE:Room", "đã tạo phòng[ \"{room}\"]"),
        Map.entry("UPDATE:Room", "đã cập nhật phòng[ \"{room}\"]"),
        Map.entry("DELETE:Room", "đã ngừng sử dụng phòng[ #{id}]"),
        Map.entry("CREATE:Subject", "đã tạo bộ môn[ \"{subject}\"]"),
        Map.entry("UPDATE:Subject", "đã cập nhật bộ môn[ \"{subject}\"]"),
        Map.entry("DELETE:Subject", "đã xoá bộ môn[ #{id}]"),
        Map.entry("CREATE:QuizQuestion", "đã thêm câu hỏi khảo sát[ \"{content}\"]"),
        Map.entry("UPDATE:QuizQuestion", "đã sửa câu hỏi khảo sát[ \"{content}\"]"),
        Map.entry("DELETE:QuizQuestion", "đã ẩn câu hỏi khảo sát[ #{id}]"),

        Map.entry("MARK_ATTENDANCE:ClassAttendance", "đã điểm danh lớp[ #{id}]"),
        Map.entry("ASSIGN_WORKOUT_PLAN:WorkoutPlan", "đã giao giáo án[ \"{plan}\"][ cho {targetName}]"),
        Map.entry("UPDATE_PLAN_STATUS:MemberPlan", "đã cập nhật tiến độ giáo án[ #{id}][ thành {status}]"),
        Map.entry("RECORD_WORKOUT_RESULT:WorkoutResult", "đã ghi kết quả tập luyện[ cho hội viên {member}]"),
        Map.entry("UPDATE_WORKOUT_RESULT:WorkoutResult", "đã sửa kết quả tập luyện[ của hội viên {member}]"),
        Map.entry("SEND_NOTIFICATION_CLASS:Notification", "đã gửi thông báo[ \"{title}\"][ tới {targetName}]")
    );

    private static final Pattern OPTIONAL = Pattern.compile("\\[([^\\]]*)]");
    private static final Pattern VAR = Pattern.compile("\\{(\\w+)}");

    static String render(String actionType, String targetEntity, Map<String, String> values) {
        String tpl = TEMPLATES.getOrDefault(actionType + ":" + targetEntity,
            "đã thực hiện " + actionType + " trên " + targetEntity + "[ #{id}]");

        // 1. bỏ đoạn [ ... ] có biến chưa có giá trị
        Matcher opt = OPTIONAL.matcher(tpl);
        StringBuilder step1 = new StringBuilder();
        while (opt.find()) {
            String part = opt.group(1);
            boolean complete = VAR.matcher(part).results().allMatch(r -> values.get(r.group(1)) != null);
            opt.appendReplacement(step1, complete ? Matcher.quoteReplacement(part) : "");
        }
        opt.appendTail(step1);

        // 2. điền giá trị vào {biến}
        Matcher var = VAR.matcher(step1);
        StringBuilder out = new StringBuilder();
        while (var.find()) {
            var.appendReplacement(out, Matcher.quoteReplacement(values.getOrDefault(var.group(1), "")));
        }
        var.appendTail(out);
        return out.toString().trim();
    }
}
