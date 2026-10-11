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

            // 2 tu dong lay ten ham vua chay
            String methodName = joinPoint.getSignature().getName();
            String details = "Thực hiện thành công hàm: " + methodName;

            // 3. ghi vao audit log
            AuditLog auditLog = AuditLog.builder()
                .user(user)
                .actionType(logAudit.actionType())
                .targetEntity(logAudit.targetEntity())
                .details(details)
                .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e){
            System.err.println("Lỗi khi tự động ghi Audit Log: " + e.getMessage());
        }
    }
}
