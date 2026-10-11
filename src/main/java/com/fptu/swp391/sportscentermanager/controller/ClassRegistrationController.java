package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.ClassRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Member tự đăng ký / hủy lớp và xem lịch cá nhân.
 * memberId luôn lấy từ token (principal), KHÔNG nhận từ client -> member không thể thao tác thay người khác.
 */
@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
public class ClassRegistrationController {

    private final ClassRegistrationService registrationService;

    @PreAuthorize("hasRole('MEMBER') and hasAuthority('REGISTER_CLASS')")
    @PostMapping("/registrations")
    public ResponseEntity<ClassRegistrationResponseDTO> register(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody ClassRegistrationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(registrationService.register(principal.getUser().getUserId(), dto.getClassId()));
    }

    @PreAuthorize("hasRole('MEMBER') and hasAuthority('REGISTER_CLASS')")
    @DeleteMapping("/registrations/{registrationId}")
    public ResponseEntity<ClassRegistrationResponseDTO> cancel(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable Long registrationId) {
        return ResponseEntity.ok(
            registrationService.cancel(principal.getUser().getUserId(), registrationId));
    }

    // Lịch sử đăng ký (cả đã hủy)
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_SCHEDULE')")
    @GetMapping("/registrations")
    public ResponseEntity<List<ClassRegistrationResponseDTO>> history(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(registrationService.getHistory(principal.getUser().getUserId()));
    }

    // Lịch tập sắp tới
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_SCHEDULE')")
    @GetMapping("/schedule")
    public ResponseEntity<List<ClassRegistrationResponseDTO>> schedule(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(
            registrationService.getUpcomingSchedule(principal.getUser().getUserId()));
    }
}
