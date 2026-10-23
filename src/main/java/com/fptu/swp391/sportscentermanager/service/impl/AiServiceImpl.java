package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.*;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.AttendanceStatus;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.AiService;
import com.fptu.swp391.sportscentermanager.service.SportClassService;
import com.fptu.swp391.sportscentermanager.service.ai.ClaudeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Trợ lý AI: dựng ngữ cảnh thật từ DB (mục tiêu, điểm danh, kết quả, giáo án, lịch, gói, lớp đang mở),
 * ưu tiên Claude; chưa có API key hoặc Claude lỗi thì dùng bộ luật dựng sẵn trên cùng dữ liệu đó.
 */
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClaudeClient claude;
    private final MemberRepository memberRepository;
    private final ClassAttendanceRepository attendanceRepository;
    private final WorkoutResultRepository resultRepository;
    private final MemberPlanRepository memberPlanRepository;
    private final ClassRegistrationRepository registrationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MembershipPackageRepository packageRepository;
    private final SportClassService sportClassService;

    // ======================= Gợi ý bài tập cho HLV =======================

    @Override
    @Transactional(readOnly = true)
    public AiSuggestionResponseDTO suggestExercises(Long memberId, AiSuggestionRequestDTO req) {
        Member m = memberRepository.findById(memberId).orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        MemberContext ctx = memberContext(m);
        String level = req.getLevel() != null && !req.getLevel().isBlank() ? req.getLevel() : ctx.derivedLevel();
        int minutes = req.getSessionMinutes() != null ? req.getSessionMinutes() : 60;
        String focus = req.getFocus() != null ? req.getFocus().trim() : "";

        String system = """
            Bạn là trợ lý chuyên môn cho huấn luyện viên tại một trung tâm thể thao ở Việt Nam.
            Nhiệm vụ: đề xuất MỘT buổi tập an toàn, thực tế, phù hợp mục tiêu, trình độ và lịch sử tập của học viên.
            Viết toàn bộ bằng tiếng Việt, ngắn gọn, dùng đơn vị số hiệp / số lần / giây.
            Không chẩn đoán y tế. Nếu dữ liệu cho thấy học viên mới tập hoặc lâu không tập, ưu tiên cường độ thấp và kỹ thuật.
            HLV sẽ đọc lại và chỉnh trước khi giao cho học viên.
            """;
        String prompt = "Thông tin học viên:\n" + ctx.describeForCoach()
            + "\nTrình độ HLV chọn: " + levelLabel(level)
            + "\nThời lượng buổi tập: " + minutes + " phút"
            + (focus.isEmpty() ? "" : "\nTrọng tâm HLV muốn: " + focus)
            + "\n\nHãy đề xuất buổi tập.";

        Optional<AiExercisePlan> fromClaude = claude.structured(system, prompt, AiExercisePlan.class);
        AiExercisePlan plan = fromClaude.orElseGet(() -> rulePlan(ctx, level, minutes, focus));

        return AiSuggestionResponseDTO.builder()
            .source(fromClaude.isPresent() ? "CLAUDE" : "RULES")
            .model(fromClaude.isPresent() ? claude.model() : null)
            .memberId(memberId)
            .memberName(ctx.name)
            .goal(ctx.goal)
            .level(level)
            .sessionMinutes(minutes)
            .basedOn(ctx.basedOn())
            .plan(plan)
            .build();
    }

    // ======================= Trợ lý hỏi đáp cho hội viên =======================

    @Override
    @Transactional(readOnly = true)
    public AiChatResponseDTO chat(Long memberId, AiChatRequestDTO req) {
        Member m = memberRepository.findById(memberId).orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        MemberContext ctx = memberContext(m);
        CenterContext center = centerContext();
        String message = req.getMessage().trim();
        List<AiChatRequestDTO.Turn> history = Optional.ofNullable(req.getHistory()).orElse(List.of()).stream()
            .filter(t -> t.getContent() != null && !t.getContent().isBlank())
            .toList();

        String system = """
            Bạn là trợ lý ảo của trung tâm thể thao Sport Center, trò chuyện với một hội viên.
            Trả lời bằng tiếng Việt, thân thiện, ngắn gọn (tối đa khoảng 8 dòng), có thể dùng gạch đầu dòng.
            Thông tin về lịch, lớp, gói tập, giá chỉ được lấy từ DỮ LIỆU bên dưới; nếu không có thì nói chưa có thông tin
            và gợi ý hội viên hỏi lễ tân. Với câu hỏi về bài tập, đưa lời khuyên an toàn và nhắc hỏi HLV khi có chấn thương.
            Hướng dẫn thao tác trên web: đặt lớp ở mục "Lớp học", xem lịch ở "Lịch tập của tôi", mua/gia hạn ở "Gói tập",
            giáo án và kết quả ở "Tập luyện", hóa đơn ở "Thanh toán".

            DỮ LIỆU HỘI VIÊN:
            """ + ctx.describeForMember() + "\n\nDỮ LIỆU TRUNG TÂM:\n" + center.describe();

        Optional<String> fromClaude = claude.chat(system, history, message);
        String reply = fromClaude.orElseGet(() -> ruleReply(message, ctx, center));
        return AiChatResponseDTO.builder()
            .source(fromClaude.isPresent() ? "CLAUDE" : "RULES")
            .reply(reply)
            .suggestions(nextQuestions(message))
            .build();
    }

    // ======================= Ngữ cảnh =======================

    private MemberContext memberContext(Member m) {
        Long id = m.getUserId();
        LocalDateTime now = LocalDateTime.now();
        MemberContext c = new MemberContext();
        c.name = m.getFirstName() + " " + m.getLastName();
        c.gender = m.getGender();
        c.goal = m.getTrainingGoal() == null || m.getTrainingGoal().isBlank() || "Chưa cập nhật".equals(m.getTrainingGoal())
            ? null : m.getTrainingGoal().trim();

        List<ClassAttendance> att = attendanceRepository.findByMember_UserId(id);
        c.present = (int) att.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        c.late = (int) att.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        c.absent = (int) att.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        c.lastAttended = att.stream()
            .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
            .map(a -> a.getSportClass().getStartTime())
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder()).orElse(null);

        c.results = resultRepository.findByMember_UserIdOrderByLoggedDateDesc(id).stream().limit(5).toList();
        c.plans = memberPlanRepository.findByMember_UserId(id).stream()
            .sorted(Comparator.comparing(MemberPlan::getAssignedDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5).toList();
        c.upcoming = registrationRepository.findUpcoming(id, now).stream()
            .filter(r -> "REGISTERED".equals(r.getStatus()))
            .map(ClassRegistration::getSportClass)
            .filter(s -> !"CANCELLED".equals(s.getStatus()))
            .sorted(Comparator.comparing(SportClass::getStartTime))
            .limit(5).toList();
        c.subscriptions = subscriptionRepository.findByMember_UserIdAndStatus(id, "ACTIVE").stream()
            .filter(s -> s.getEndDate() != null && s.getEndDate().isAfter(now))
            .sorted(Comparator.comparing(Subscription::getEndDate))
            .toList();
        return c;
    }

    private CenterContext centerContext() {
        LocalDateTime now = LocalDateTime.now();
        CenterContext c = new CenterContext();
        c.packages = packageRepository.findAll().stream()
            .filter(p -> "ACTIVE".equals(p.getStatus()))
            .sorted(Comparator.comparing(MembershipPackage::getDurationDays))
            .toList();
        c.openClasses = sportClassService.getAllClasses().stream()
            .filter(s -> "OPENING".equals(s.getStatus()) && s.getStartTime() != null)
            .filter(s -> s.getStartTime().isAfter(now) && s.getStartTime().isBefore(now.plusDays(14)))
            .filter(s -> s.getAvailableSeats() == null || s.getAvailableSeats() > 0)
            .sorted(Comparator.comparing(SportClassResponseDTO::getStartTime))
            .limit(8).toList();
        return c;
    }

    private static String money(double v) {
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(Math.round(v)) + " đ";
    }

    private static String levelLabel(String level) {
        return switch (level) {
            case "ADVANCED" -> "Nâng cao";
            case "INTERMEDIATE" -> "Trung bình";
            default -> "Cơ bản";
        };
    }

    // bỏ dấu + chữ thường để dò ý định câu hỏi tiếng Việt
    private static String norm(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }

    private static boolean has(String text, String... words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }

    private static class MemberContext {
        String name;
        String gender;
        String goal;
        int present;
        int late;
        int absent;
        LocalDateTime lastAttended;
        List<WorkoutResult> results = List.of();
        List<MemberPlan> plans = List.of();
        List<SportClass> upcoming = List.of();
        List<Subscription> subscriptions = List.of();

        int attended() { return present + late; }

        // Suy trình độ từ số buổi đã tập
        String derivedLevel() {
            if (attended() >= 12) return "ADVANCED";
            if (attended() >= 4) return "INTERMEDIATE";
            return "BEGINNER";
        }

        Optional<Subscription> current() {
            LocalDateTime now = LocalDateTime.now();
            return subscriptions.stream().filter(s -> !s.getStartDate().isAfter(now)).findFirst();
        }

        LocalDateTime coveredUntil() {
            return subscriptions.stream().map(Subscription::getEndDate).max(Comparator.naturalOrder()).orElse(null);
        }

        List<String> basedOn() {
            List<String> b = new ArrayList<>();
            b.add("Mục tiêu: " + (goal != null ? goal : "chưa cập nhật"));
            b.add("Đã tập " + attended() + " buổi (có mặt " + present + ", muộn " + late + ", vắng " + absent + ")");
            if (lastAttended != null) b.add("Buổi gần nhất: " + lastAttended.format(D));
            if (!results.isEmpty()) b.add(results.size() + " kết quả tập gần nhất");
            if (!plans.isEmpty()) b.add(plans.size() + " giáo án đã giao");
            return b;
        }

        String describeForCoach() {
            StringBuilder s = new StringBuilder();
            s.append("- Tên: ").append(name).append(", giới tính: ").append(gender == null ? "không rõ" : gender).append('\n');
            s.append("- Mục tiêu tập luyện: ").append(goal != null ? goal : "chưa cập nhật").append('\n');
            s.append("- Điểm danh: có mặt ").append(present).append(", đến muộn ").append(late).append(", vắng ").append(absent).append('\n');
            if (lastAttended != null) s.append("- Buổi tập gần nhất: ").append(lastAttended.format(D)).append('\n');
            if (!results.isEmpty()) {
                s.append("- Kết quả tập gần đây (mới nhất trước):\n");
                results.forEach(r -> s.append("  * ").append(r.getLoggedDate() != null ? r.getLoggedDate().format(D) + ": " : "")
                    .append(r.getActivityDetails())
                    .append(r.getCoachFeedback() != null && !r.getCoachFeedback().isBlank() ? " | Nhận xét HLV: " + r.getCoachFeedback() : "")
                    .append('\n'));
            }
            if (!plans.isEmpty()) {
                s.append("- Giáo án đã giao:\n");
                plans.forEach(p -> s.append("  * ").append(p.getWorkoutPlan().getPlanName()).append(" (").append(p.getStatus()).append("): ")
                    .append(p.getWorkoutPlan().getExerciseDetails()).append('\n'));
            }
            return s.toString();
        }

        String describeForMember() {
            StringBuilder s = new StringBuilder(describeForCoach());
            Optional<Subscription> cur = current();
            if (cur.isPresent()) {
                s.append("- Gói đang dùng: ").append(cur.get().getMembershipPackage().getPackageName())
                    .append(", hết hạn ").append(cur.get().getEndDate().format(D)).append('\n');
                LocalDateTime until = coveredUntil();
                if (until != null && until.isAfter(cur.get().getEndDate())) {
                    s.append("- Đã gia hạn nối tiếp đến ").append(until.format(D)).append('\n');
                }
            } else {
                s.append("- Chưa có gói tập còn hiệu lực\n");
            }
            if (upcoming.isEmpty()) s.append("- Chưa có lịch tập sắp tới\n");
            else {
                s.append("- Lịch tập sắp tới:\n");
                upcoming.forEach(c -> s.append("  * ").append(c.getSubject().getSubjectName()).append(" lúc ").append(c.getStartTime().format(DT))
                    .append(", phòng ").append(c.getRoom() != null ? c.getRoom().getRoomName() : "?")
                    .append(", HLV ").append(c.getCoach() != null ? c.getCoach().getFirstName() + " " + c.getCoach().getLastName() : "chưa phân công")
                    .append('\n'));
            }
            return s.toString();
        }
    }

    private static class CenterContext {
        List<MembershipPackage> packages = List.of();
        List<SportClassResponseDTO> openClasses = List.of();

        String describe() {
            StringBuilder s = new StringBuilder("- Gói tập đang bán:\n");
            packages.forEach(p -> s.append("  * ").append(p.getPackageName()).append(": ").append(money(p.getPrice()))
                .append(" / ").append(p.getDurationDays()).append(" ngày")
                .append(p.getDescription() != null && !p.getDescription().isBlank() ? " (" + p.getDescription() + ")" : "")
                .append('\n'));
            s.append("- Thanh toán: VNPay online hoặc tại quầy (tiền mặt, chuyển khoản, thẻ, ví điện tử); gia hạn được cộng nối tiếp.\n");
            if (openClasses.isEmpty()) s.append("- Chưa có lớp mở đăng ký trong 14 ngày tới\n");
            else {
                s.append("- Lớp còn chỗ trong 14 ngày tới:\n");
                openClasses.forEach(c -> s.append("  * ").append(c.getSubjectName()).append(" lúc ").append(c.getStartTime().format(DT))
                    .append(", HLV ").append(c.getCoachName()).append(", phòng ").append(c.getRoomName())
                    .append(", còn ").append(c.getAvailableSeats()).append(" chỗ\n"));
            }
            return s.toString();
        }
    }

    // ======================= Bộ luật dự phòng: gợi ý bài tập =======================

    private enum GoalType { FAT_LOSS, MUSCLE, FLEXIBILITY, ENDURANCE, GENERAL }

    private static GoalType goalType(String text) {
        String t = norm(text);
        if (has(t, "giam can", "giam mo", "giam beo", "dot mo", "cardio", "eo thon", "giam vong")) return GoalType.FAT_LOSS;
        if (has(t, "tang co", "co bap", "the hinh", "suc manh", "tang can", "co bung", "6 mui", "gym", "nang ta")) return GoalType.MUSCLE;
        if (has(t, "deo", "yoga", "gian co", "thu gian", "dau lung", "cot song", "tu the", "linh hoat")) return GoalType.FLEXIBILITY;
        if (has(t, "suc ben", "chay", "boi", "marathon", "the luc", "hit tho", "tim mach")) return GoalType.ENDURANCE;
        return GoalType.GENERAL;
    }

    private AiExercisePlan rulePlan(MemberContext ctx, String level, int minutes, String focus) {
        // trọng tâm HLV nhập được ưu tiên; không rõ thì mới suy từ mục tiêu của học viên
        GoalType byFocus = focus.isEmpty() ? GoalType.GENERAL : goalType(focus);
        GoalType type = byFocus != GoalType.GENERAL ? byFocus : goalType(ctx.goal == null ? "" : ctx.goal);
        int lv = switch (level) { case "ADVANCED" -> 2; case "INTERMEDIATE" -> 1; default -> 0; };
        String sets = String.valueOf(2 + lv);
        String rest = lv == 2 ? "45 giây" : lv == 1 ? "60 giây" : "75 giây";

        List<AiExercisePlan.Exercise> pool = switch (type) {
            case FAT_LOSS -> List.of(
                ex("Jumping jack", sets, lv == 0 ? "30 giây" : "45 giây", rest, "Tiếp đất nhẹ bằng mũi chân"),
                ex("Squat (Bodyweight / Goblet squat)", sets, lv == 2 ? "15 lần, cầm tạ 10-12kg" : "12-15 lần", rest, "Đẩy hông ra sau, gối cùng hướng mũi chân"),
                ex("Mountain climber", sets, lv == 0 ? "20 giây" : "40 giây", rest, "Giữ hông thấp, siết bụng"),
                ex("Hít đất (Push-up)", sets, lv == 0 ? "8 lần (chống gối)" : "10-15 lần", rest, "Thân người thẳng một đường"),
                ex(lv == 0 ? "Bước lên bục (Step-up)" : "Burpee", sets, lv == 0 ? "10 lần mỗi chân" : "10 lần", rest, "Giữ nhịp thở đều"),
                ex("Plank", sets, lv == 0 ? "20 giây" : lv == 1 ? "40 giây" : "60 giây", "30 giây", "Không võng lưng"),
                ex("Đạp xe / chạy biến tốc", "1", lv == 0 ? "8 phút" : "12 phút (30s nhanh / 60s chậm)", "-", "Theo dõi nhịp tim, nói chuyện được ở pha chậm"));
            case MUSCLE -> List.of(
                ex("Squat (Back squat / Goblet squat)", String.valueOf(3 + lv), lv == 0 ? "10 lần, tạ nhẹ" : "8-10 lần", "90 giây", "Ưu tiên kỹ thuật, tăng tạ 2.5kg khi làm đủ số lần"),
                ex("Đẩy ngực (Bench press / Push-up)", String.valueOf(3 + lv), lv == 0 ? "10 lần" : "8-10 lần", "90 giây", "Hạ chậm 2 giây"),
                ex("Romanian deadlift", sets, "10 lần", "90 giây", "Lưng thẳng, cảm nhận đùi sau"),
                ex("Kéo cáp / chèo tạ (Row)", sets, "10-12 lần", rest, "Siết bả vai khi kéo"),
                ex("Đẩy vai (Overhead press)", sets, "10 lần", rest, "Siết mông, không ưỡn lưng"),
                ex("Plank nghiêng (Side plank)", sets, lv == 0 ? "20 giây mỗi bên" : "40 giây mỗi bên", "30 giây", "Hông nâng cao"));
            case FLEXIBILITY -> List.of(
                ex("Mèo - bò (Cat-cow)", "2", "10 nhịp thở", "-", "Chuyển động theo hơi thở"),
                ex("Chó úp mặt (Downward dog)", sets, "30-45 giây", "20 giây", "Gập hông, kéo dài cột sống"),
                ex("Mở hông (Low lunge)", sets, "30 giây mỗi bên", "20 giây", "Không đẩy gối quá mũi chân"),
                ex("Giãn đùi sau (Hamstring stretch)", sets, "40 giây mỗi bên", "20 giây", "Không nhún"),
                ex("Bird-dog", sets, "8 lần mỗi bên", "30 giây", "Giữ lưng phẳng, siết bụng"),
                ex("Tư thế em bé (Child pose)", "1", "60 giây", "-", "Thả lỏng vai và lưng dưới"));
            case ENDURANCE -> List.of(
                ex("Chạy / đạp xe nhịp đều", "1", lv == 0 ? "10 phút" : lv == 1 ? "15 phút" : "20 phút", "-", "Cường độ vừa, thở bằng mũi được"),
                ex("Lunge đi bộ (Walking lunge)", sets, "10 bước mỗi chân", rest, "Thân trên thẳng"),
                ex("Nhảy dây", sets, lv == 0 ? "40 giây" : "60 giây", rest, "Nhảy thấp, cổ tay xoay"),
                ex("Step-up", sets, "12 lần mỗi chân", rest, "Đẩy bằng gót chân"),
                ex("Plank", sets, lv == 0 ? "20 giây" : "45 giây", "30 giây", "Thở đều"),
                ex("Chạy biến tốc", "1", lv == 0 ? "5 x (30s nhanh / 90s chậm)" : "8 x (40s nhanh / 60s chậm)", "-", "Dừng nếu chóng mặt"));
            default -> List.of(
                ex("Squat", sets, "12 lần", rest, "Gối cùng hướng mũi chân"),
                ex("Hít đất (Push-up)", sets, lv == 0 ? "8 lần (chống gối)" : "12 lần", rest, "Siết bụng"),
                ex("Chèo tạ đơn (Dumbbell row)", sets, "10 lần mỗi tay", rest, "Lưng thẳng"),
                ex("Lunge", sets, "10 lần mỗi chân", rest, "Bước dài vừa phải"),
                ex("Plank", sets, lv == 0 ? "20 giây" : "40 giây", "30 giây", "Không võng lưng"),
                ex("Đạp xe nhẹ", "1", "8 phút", "-", "Hồi phục"));
        };
        // ~8-10 phút cho mỗi bài chính sau khi trừ khởi động + giãn cơ
        int count = Math.max(3, Math.min(pool.size(), (minutes - 15) / 8));
        List<AiExercisePlan.Exercise> exercises = pool.subList(0, count);

        String goalText = ctx.goal != null ? ctx.goal : "nâng cao thể lực chung";
        List<String> advice = new ArrayList<>();
        advice.add("Học viên đã tập " + ctx.attended() + " buổi, áp dụng mức " + levelLabel(level).toLowerCase() + "; tăng khối lượng khoảng 5-10% mỗi tuần nếu hoàn thành tốt.");
        if (ctx.lastAttended == null || ctx.lastAttended.isBefore(LocalDateTime.now().minusDays(14))) {
            advice.add("Lâu chưa tập hoặc mới bắt đầu: buổi đầu giảm 1 hiệp mỗi bài và theo dõi phản ứng cơ thể.");
        }
        if (ctx.absent > ctx.attended()) {
            advice.add("Tỉ lệ vắng cao: nên trao đổi lịch phù hợp và giao bài tập ngắn tại nhà để giữ thói quen.");
        }
        ctx.results.stream().findFirst().ifPresent(r -> advice.add("Kết quả gần nhất: \"" + r.getActivityDetails() + "\""
            + (r.getCoachFeedback() != null && !r.getCoachFeedback().isBlank() ? ", nhận xét: \"" + r.getCoachFeedback() + "\"" : "") + ". Dùng làm mốc để điều chỉnh tạ / thời gian."));
        advice.add("Dừng bài tập nếu đau khớp, chóng mặt hoặc khó thở bất thường.");

        String title = switch (type) {
            case FAT_LOSS -> "Đốt mỡ toàn thân";
            case MUSCLE -> "Sức mạnh & tăng cơ";
            case FLEXIBILITY -> "Dẻo dai & phục hồi";
            case ENDURANCE -> "Sức bền tim mạch";
            default -> "Thể lực tổng quát";
        } + " - " + levelLabel(level).toLowerCase();

        return new AiExercisePlan(
            title,
            "Buổi " + minutes + " phút cho mục tiêu \"" + goalText + "\"" + (focus.isEmpty() ? "" : ", trọng tâm " + focus) + ", " + exercises.size() + " bài chính.",
            List.of("5 phút đi bộ nhanh hoặc đạp xe nhẹ", "Xoay khớp cổ, vai, hông, gối, cổ chân", "10 lần squat không tạ + 10 lần mở ngực"),
            exercises,
            List.of("Đi bộ chậm 3 phút cho nhịp tim hạ dần", "Giãn đùi trước, đùi sau, bắp chân mỗi bên 30 giây", "Giãn ngực và vai 30 giây, hít thở sâu"),
            advice);
    }

    private static AiExercisePlan.Exercise ex(String name, String sets, String reps, String rest, String note) {
        return new AiExercisePlan.Exercise(name, sets, reps, rest, note);
    }

    // ======================= Bộ luật dự phòng: hỏi đáp =======================

    private String ruleReply(String message, MemberContext ctx, CenterContext center) {
        String t = norm(message);
        StringBuilder r = new StringBuilder();
        // hỏi lớp đang mở ("lớp nào còn chỗ tuần này") phải trả lời lớp, không nhầm sang lịch cá nhân vì có chữ "tuần này"
        boolean askOpenClasses = has(t, "con cho", "lop nao", "lop gi", "mon nao", "dat lop", "dang ky lop", "lop moi", "lop trong")
            && !has(t, "cua toi", "cua minh", "lich tap cua");
        // hỏi về gói ("khi nào gói hết hạn") ưu tiên hơn lịch tập dù có chữ "khi nào"
        boolean askPackage = has(t, "goi", "het han", "gia han", "bao nhieu tien", "hoc phi");

        if (!askOpenClasses && !askPackage && has(t, "lich", "khi nao", "buoi tap", "sap toi", "hom nay", "ngay mai", "tuan nay")) {
            if (ctx.upcoming.isEmpty()) {
                r.append("Bạn chưa có lịch tập sắp tới. Vào mục \"Lớp học\" để đặt lớp nhé.");
                appendOpenClasses(r, center, 3);
            } else {
                r.append("Lịch tập sắp tới của bạn:\n");
                ctx.upcoming.forEach(c -> r.append("• ").append(c.getSubject().getSubjectName()).append(" - ").append(c.getStartTime().format(DT))
                    .append(", ").append(c.getRoom() != null ? c.getRoom().getRoomName() : "")
                    .append(c.getCoach() != null ? ", HLV " + c.getCoach().getFirstName() + " " + c.getCoach().getLastName() : "").append('\n'));
                r.append("Bạn có thể hủy trước giờ học ở mục \"Lịch tập của tôi\".");
            }
            return r.toString().trim();
        }
        if (!askOpenClasses && has(t, "goi", "gia", "bao nhieu tien", "phi", "het han", "gia han", "mua")) {
            Optional<Subscription> cur = ctx.current();
            if (cur.isPresent()) {
                r.append("Gói hiện tại: ").append(cur.get().getMembershipPackage().getPackageName()).append(", hết hạn ").append(cur.get().getEndDate().format(D)).append(".\n");
                LocalDateTime until = ctx.coveredUntil();
                if (until != null && until.isAfter(cur.get().getEndDate())) r.append("Bạn đã gia hạn nối tiếp đến ").append(until.format(D)).append(".\n");
            } else {
                r.append("Bạn chưa có gói tập còn hiệu lực.\n");
            }
            r.append("Các gói đang bán:\n");
            center.packages.forEach(p -> r.append("• ").append(p.getPackageName()).append(": ").append(money(p.getPrice())).append(" / ").append(p.getDurationDays()).append(" ngày\n"));
            r.append("Mua hoặc gia hạn ở mục \"Gói tập\" (VNPay) hoặc tại quầy lễ tân; ngày còn lại được cộng nối tiếp.");
            return r.toString().trim();
        }
        if (has(t, "lop", "mon", "dang ky", "dat cho", "con cho", "yoga", "gym", "boi", "boxing")) {
            r.append("Các lớp còn chỗ trong 14 ngày tới:");
            appendOpenClasses(r, center, 5);
            r.append("\nĐặt lớp ở mục \"Lớp học\" (cần gói tập còn hạn đến ngày học).");
            return r.toString().trim();
        }
        if (has(t, "bai tap", "tap gi", "giao an", "ket qua", "tien do", "nen tap", "tap the nao", "dinh duong", "an uong")) {
            if (!ctx.plans.isEmpty()) {
                r.append("Giáo án HLV đã giao cho bạn:\n");
                ctx.plans.stream().limit(3).forEach(p -> r.append("• ").append(p.getWorkoutPlan().getPlanName()).append(": ").append(p.getWorkoutPlan().getExerciseDetails()).append('\n'));
            }
            ctx.results.stream().findFirst().ifPresent(res -> r.append("Kết quả gần nhất: ").append(res.getActivityDetails())
                .append(res.getCoachFeedback() != null && !res.getCoachFeedback().isBlank() ? " (HLV nhận xét: " + res.getCoachFeedback() + ")" : "").append(".\n"));
            GoalType type = goalType(ctx.goal == null ? message : ctx.goal + " " + message);
            r.append(switch (type) {
                case FAT_LOSS -> "Gợi ý chung để giảm mỡ: 3-4 buổi/tuần, kết hợp bài toàn thân (squat, hít đất, mountain climber) với cardio biến tốc 10-15 phút, ngủ đủ và ăn đủ đạm.";
                case MUSCLE -> "Gợi ý chung để tăng cơ: tập kháng lực 3-4 buổi/tuần, mỗi bài 3-4 hiệp 8-12 lần, tăng tạ từ từ, ăn đủ đạm (khoảng 1.6g/kg cân nặng) và nghỉ 48 giờ cho mỗi nhóm cơ.";
                case FLEXIBILITY -> "Gợi ý chung để dẻo dai: giãn cơ 10-15 phút mỗi ngày, yoga 2-3 buổi/tuần, giữ mỗi tư thế 30-45 giây và không nhún.";
                case ENDURANCE -> "Gợi ý chung để tăng sức bền: 3 buổi cardio/tuần (1 buổi dài nhịp đều, 1 buổi biến tốc), thêm 2 buổi sức mạnh chân và core.";
                default -> "Gợi ý chung: 3 buổi/tuần toàn thân, mỗi buổi khởi động 5-10 phút, 4-6 bài chính và giãn cơ cuối buổi.";
            });
            r.append("\nNếu có chấn thương, hãy hỏi HLV trước khi tập.");
            return r.toString().trim();
        }
        if (has(t, "hlv", "huan luyen vien", "coach", "thay", "co giao")) {
            Set<String> coaches = new LinkedHashSet<>();
            ctx.upcoming.forEach(c -> { if (c.getCoach() != null) coaches.add(c.getCoach().getFirstName() + " " + c.getCoach().getLastName() + (c.getCoach().getSpeciality() != null ? " (" + c.getCoach().getSpeciality() + ")" : "")); });
            if (coaches.isEmpty()) center.openClasses.forEach(c -> coaches.add(c.getCoachName() + (c.getCoachSpeciality() != null ? " (" + c.getCoachSpeciality() + ")" : "")));
            r.append(coaches.isEmpty() ? "Hiện chưa có thông tin HLV cho lịch sắp tới." : "HLV phụ trách các lớp của bạn / lớp sắp mở:\n• " + String.join("\n• ", coaches));
            r.append("\nBấm vào tên HLV trong thẻ lớp để xem chuyên môn.");
            return r.toString().trim();
        }
        if (has(t, "thanh toan", "vnpay", "hoa don", "tien mat", "chuyen khoan", "hoan tien")) {
            return "Bạn có thể thanh toán online qua VNPay (thẻ ATM, thẻ quốc tế, QR) ở mục \"Gói tập\", hoặc tại quầy bằng tiền mặt, chuyển khoản, thẻ, ví điện tử. "
                + "Hóa đơn xem và in ở mục \"Thanh toán\". Cần hoàn tiền vui lòng liên hệ lễ tân.";
        }
        if (has(t, "xin chao", "chao", "hello", "hi ")) {
            return "Chào " + ctx.name + "! Mình là trợ lý của Sport Center. Bạn có thể hỏi về lịch tập, lớp còn chỗ, gói tập và giá, bài tập phù hợp mục tiêu hoặc cách thanh toán.";
        }
        return "Mình chưa hiểu rõ câu hỏi. Bạn có thể hỏi:\n• \"Lịch tập tuần này của tôi?\"\n• \"Lớp nào còn chỗ?\"\n• \"Gói tập giá bao nhiêu?\"\n• \"Tôi nên tập gì để " + (ctx.goal != null ? ctx.goal.toLowerCase() : "khỏe hơn") + "?\"\nCâu hỏi khác vui lòng liên hệ quầy lễ tân.";
    }

    private static void appendOpenClasses(StringBuilder r, CenterContext center, int max) {
        if (center.openClasses.isEmpty()) {
            r.append("\nHiện chưa có lớp mở đăng ký trong 14 ngày tới.");
            return;
        }
        center.openClasses.stream().limit(max).forEach(c -> r.append("\n• ").append(c.getSubjectName()).append(" - ").append(c.getStartTime().format(DT))
            .append(", HLV ").append(c.getCoachName()).append(", còn ").append(c.getAvailableSeats()).append(" chỗ"));
    }

    private static List<String> nextQuestions(String message) {
        String t = norm(message);
        List<String> all = new ArrayList<>(List.of("Lịch tập sắp tới của tôi?", "Lớp nào còn chỗ tuần này?", "Gói tập giá bao nhiêu?", "Tôi nên tập gì cho mục tiêu của mình?"));
        all.removeIf(q -> {
            String n = norm(q);
            return (has(t, "lich") && n.contains("lich")) || (has(t, "lop") && n.contains("lop")) || (has(t, "goi", "gia") && n.contains("goi")) || (has(t, "tap gi", "bai tap") && n.contains("tap gi"));
        });
        return all.stream().limit(3).collect(Collectors.toList());
    }
}
