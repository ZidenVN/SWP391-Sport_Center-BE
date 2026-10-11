package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.AuditLogResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.AuditLog;
import com.fptu.swp391.sportscentermanager.repository.AuditLogRepository;
import com.fptu.swp391.sportscentermanager.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository auditLogRepository;
    @Override
    public Page<AuditLogResponseDTO> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(this::mapToResponse);
    }

    private AuditLogResponseDTO mapToResponse(AuditLog log){
        return AuditLogResponseDTO.builder()
            .logId(log.getLogId())
            .userId(log.getUser() != null ? log.getUser().getUserId() : null)
            .email(log.getUser() != null ? log.getUser().getEmail() : null)
            .actionType(log.getActionType())
            .targetEntity(log.getTargetEntity())
            .targetEntityId(log.getTargetEntityId())
            .details(log.getDetails())
            .createdAt(log.getCreatedAt())
            .build();
    }
}
