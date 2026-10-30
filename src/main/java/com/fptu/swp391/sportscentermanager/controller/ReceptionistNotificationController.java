package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.ReceptionistNotificationRequestDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.ReceptionistNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Lễ tân gửi thông báo cho 1 / nhiều / tất cả member đang ACTIVE. */
@RestController
@RequestMapping("/api/v1/receptionist/notifications")
@RequiredArgsConstructor
public class ReceptionistNotificationController {

    private final ReceptionistNotificationService receptionistNotificationService;

    @LogAudit(actionType = "SEND_NOTIFICATION", targetEntity = "Notification")
    @PreAuthorize("hasAuthority('SEND_NOTIFICATION')")
    @PostMapping
    public ResponseEntity<Map<String, Integer>> send(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody ReceptionistNotificationRequestDTO dto) {
        int count = receptionistNotificationService.send(principal.getUser().getUserId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("recipientCount", count));
    }
}
