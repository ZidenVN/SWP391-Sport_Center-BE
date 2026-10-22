package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.AuditLogResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AuditLogService {
    Page<AuditLogResponseDTO> getAllLogs(Pageable pageable);

    // BE-26: lọc nhật ký
    Page<AuditLogResponseDTO> searchLogs(LocalDate fromDate, LocalDate toDate, String actionType,
                                         String targetEntity, Long userId, Pageable pageable);
}
