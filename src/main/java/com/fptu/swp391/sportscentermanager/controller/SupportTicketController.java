package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.SupportTicketRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SupportTicketResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Lễ tân tiếp nhận và ghi nhận yêu cầu hỗ trợ từ member. */
@RestController
@RequestMapping("/api/v1/receptionist/tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService ticketService;

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('MANAGE_TICKET')")
    @PostMapping
    public ResponseEntity<SupportTicketResponseDTO> create(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody SupportTicketRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ticketService.create(principal.getUser().getUserId(), dto));
    }

    // ?memberId=... để lọc theo member, bỏ trống để xem tất cả
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('MANAGE_TICKET')")
    @GetMapping
    public ResponseEntity<List<SupportTicketResponseDTO>> list(
        @RequestParam(required = false) Long memberId) {
        return ResponseEntity.ok(ticketService.list(memberId));
    }

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('MANAGE_TICKET')")
    @PatchMapping("/{ticketId}/resolve")
    public ResponseEntity<SupportTicketResponseDTO> resolve(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.resolve(ticketId));
    }
}
