package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.AuditLogResponseDTO;
import com.fptu.swp391.sportscentermanager.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogService auditLogService;

    @PreAuthorize("hasRole('CENTER_MANAGER')")
    @GetMapping
    public ResponseEntity<Page<AuditLogResponseDTO>> getAllAuditLogs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page,size, Sort.by(Sort.Direction.DESC,"createdAt"));
        return ResponseEntity.ok(auditLogService.getAllLogs(pageable));
    }

}
