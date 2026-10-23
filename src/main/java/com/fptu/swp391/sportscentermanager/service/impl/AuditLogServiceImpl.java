package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.AuditLogResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.AuditLog;
import com.fptu.swp391.sportscentermanager.repository.AuditLogRepository;
import com.fptu.swp391.sportscentermanager.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import java.time.LocalDate;
import java.time.LocalTime;
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository auditLogRepository;
    @Override
    public Page<AuditLogResponseDTO> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<AuditLogResponseDTO> searchLogs(LocalDate fromDate, LocalDate toDate, String actionType,
                                                String targetEntity, Long userId, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
        return auditLogRepository.search(
                fromDate != null ? fromDate.atStartOfDay() : null,
                toDate != null ? toDate.atTime(LocalTime.MAX) : null,
                blankToNull(actionType), blankToNull(targetEntity), userId, pageable)
            .map(this::mapToResponse);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
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
