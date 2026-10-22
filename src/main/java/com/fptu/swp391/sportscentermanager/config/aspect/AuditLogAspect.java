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
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

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

            // 3. ghi vao audit log
            AuditLog auditLog = AuditLog.builder()
                .user(user)
                .actionType(logAudit.actionType())
                .targetEntity(logAudit.targetEntity())
                .targetEntityId(targetId)
                .details(details.toString())
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
}
