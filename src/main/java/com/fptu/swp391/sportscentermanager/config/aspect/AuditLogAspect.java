package com.fptu.swp391.sportscentermanager.config.aspect;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.entity.AuditLog;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.repository.AuditLogRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @AfterReturning(pointcut = "@annotation(logAudit)", returning = "result")
    public void logAction(JoinPoint joinPoint, LogAudit logAudit, Object result) {
        try{
            // 1 lay thong tin account dang thuc hien requst tu Sercurity context (jwt)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = null;
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                UserDetails userDetails = (UserDetails) auth.getPrincipal();
                // tim user that trong DB bang email tu token
                user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            }

            // 2 tu dong lay ten ham vua chay + cac tham so don gian (id, status...). Khong ghi body/mat khau.
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getName();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            StringBuilder details = new StringBuilder("Thực hiện thành công hàm: " + methodName);
            Long targetId = null;
            for (int i = 0; paramNames != null && i < paramNames.length; i++) {
                Object arg = args[i];
                if (!isSimpleValue(arg) || paramNames[i].toLowerCase().contains("password")) continue;
                details.append(" | ").append(paramNames[i]).append("=").append(arg);
                // BE-26: id dau tien tren URL (id, classId, paymentId...) la doi tuong bi tac dong
                if (targetId == null && arg instanceof Long l
                    && (paramNames[i].equals("id") || paramNames[i].endsWith("Id"))) {
                    targetId = l;
                }
            }
            // API tao moi khong co id tren URL: lay id tu ket qua tra ve
            if (targetId == null) {
                targetId = extractId(result, logAudit.targetEntity());
            }

            // 2b. câu mô tả dễ hiểu: "<Vai trò> <Họ tên> đã ..."
            String description = buildActor(user, auth) + " "
                + AuditMessages.render(logAudit.actionType(), logAudit.targetEntity(),
                    collectValues(result, paramNames, args, targetId, logAudit.targetEntity(), user));

            // 3. ghi vao audit log
            AuditLog auditLog = AuditLog.builder()
                .user(user)
                .actionType(logAudit.actionType())
                .targetEntity(logAudit.targetEntity())
                .targetEntityId(targetId)
                .details(details.toString())
                .description(description)
                .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e){
            System.err.println("Lỗi khi tự động ghi Audit Log: " + e.getMessage());
        }
    }

    private boolean isSimpleValue(Object arg) {
        return arg instanceof Number || arg instanceof String || arg instanceof Boolean || arg instanceof Enum<?>;
    }

    // Thu goi get{Entity}Id() roi getId() tren body tra ve, vd CoachResponseDTO.getCoachId()
    private Long extractId(Object result, String targetEntity) {
        Object body = result instanceof ResponseEntity<?> re ? re.getBody() : result;
        if (body == null) return null;
        for (String getter : new String[] {"get" + targetEntity + "Id", "getId", "getUserId"}) {
            try {
                Method m = body.getClass().getMethod(getter);
                Object value = m.invoke(body);
                if (value instanceof Long l) return l;
            } catch (NoSuchMethodException ignored) {
                // thu getter tiep theo
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // Vai trò lấy từ quyền trong token (ROLE_xxx) để không phải tải Role LAZY ngoài transaction
    private String buildActor(User user, Authentication auth) {
        if (user == null) return "Khách";
        String role = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> AuditMessages.ROLES.getOrDefault(a.substring(5), ""))
            .findFirst().orElse("");
        return (role + " " + fullName(user.getFirstName(), user.getLastName())).trim();
    }

    // Gom giá trị cho mẫu câu: tham số trên URL + các field có tên trong body trả về
    private static final java.util.Set<String> PERSON_ENTITIES = java.util.Set.of("User", "Member", "Coach", "Receptionist");

    private Map<String, String> collectValues(Object result, String[] paramNames, Object[] args, Long targetId, String targetEntity, User actor) {
        Map<String, String> v = new HashMap<>();
        Long memberId = null;
        for (int i = 0; paramNames != null && i < paramNames.length; i++) {
            if (!isSimpleValue(args[i]) || paramNames[i].toLowerCase().contains("password")) continue;
            v.put(paramNames[i], String.valueOf(args[i])); // vd status, reason
            if (paramNames[i].equals("memberId") && args[i] instanceof Long l) memberId = l;
        }
        if (targetId != null) v.put("id", String.valueOf(targetId));

        Object body = result instanceof ResponseEntity<?> re ? re.getBody() : result;
        put(v, "package", read(body, "getPackageName"));
        put(v, "member", read(body, "getMemberName"));
        put(v, "subject", read(body, "getSubjectName"));
        put(v, "room", read(body, "getRoomName"));
        put(v, "coach", read(body, "getCoachName"));
        put(v, "plan", read(body, "getPlanName"));
        put(v, "title", read(body, "getTitle"));
        put(v, "targetName", read(body, "getTargetName"));
        put(v, "role", read(body, "getRoleName"));
        Object content = read(body, "getContent");
        if (content != null) v.put("content", shorten(content.toString()));
        Object first = read(body, "getFirstName"), last = read(body, "getLastName");
        if (first == null && last == null) {
            // vd đăng ký tài khoản: body trả về không có tên -> lấy từ request body (không đọc mật khẩu)
            for (Object arg : args) {
                if (arg == null || isSimpleValue(arg)) continue;
                first = read(arg, "getFirstName");
                last = read(arg, "getLastName");
                if (first != null || last != null) break;
            }
        }
        if (first != null || last != null) v.put("name", fullName(first, last));
        if (read(body, "getStartTime") instanceof LocalDateTime t) v.put("time", t.format(TIME_FMT));

        String status = String.valueOf(read(body, "getStatus"));
        v.put("toggle", "INACTIVE".equals(status) ? "khoá" : "ACTIVE".equals(status) ? "mở khoá" : "đổi trạng thái");

        // tên hội viên luôn tra theo id để cùng một kiểu "Tên Họ" (DTO thanh toán ghép ngược thứ tự)
        if (memberId == null && read(body, "getMemberId") instanceof Long l) memberId = l;
        if (memberId != null) {
            userRepository.findById(memberId).ifPresent(u -> v.put("member", fullName(u.getFirstName(), u.getLastName())));
        }

        // khoá / sửa tài khoản mà body không có tên (API trả chuỗi) -> tra theo id trên URL
        if (v.get("name") == null && targetId != null && PERSON_ENTITIES.contains(targetEntity)) {
            userRepository.findById(targetId).ifPresent(u -> {
                v.put("name", fullName(u.getFirstName(), u.getLastName()));
                if (u.getStatus() != null) {
                    v.put("toggle", u.getStatus().name().equals("INACTIVE") ? "khoá" : "mở khoá");
                }
            });
        }
        // hội viên tự thao tác cho mình -> bỏ "cho hội viên X" để khỏi lặp tên
        if (actor != null && memberId != null && memberId.equals(actor.getUserId())) v.remove("member");
        return v;
    }

    private Object read(Object body, String getter) {
        if (body == null || body instanceof String || body instanceof Iterable<?>) return null;
        try {
            return body.getClass().getMethod(getter).invoke(body);
        } catch (Exception e) {
            return null; // DTO không có field này
        }
    }

    private void put(Map<String, String> v, String key, Object value) {
        if (value != null && !value.toString().isBlank()) v.put(key, value.toString());
    }

    private String fullName(Object first, Object last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }

    private String shorten(String s) {
        return s.length() > 60 ? s.substring(0, 57) + "..." : s;
    }
}
