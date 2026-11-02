package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.EligibleClassResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MemberSessionResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.SwitchClassRequestDTO;
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
 * Member tự đăng ký / đổi / hủy lớp và xem lịch cá nhân.
 * memberId luôn lấy từ token (principal), KHÔNG nhận từ client -> member không thể thao tác thay người khác.
 */
@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
public class ClassRegistrationController {

    private final ClassRegistrationService registrationService;

    // Lớp đăng ký / đổi sang được của 1 gói lớp
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('REGISTER_CLASS')")
    @GetMapping("/subscriptions/{subscriptionId}/eligible-classes")
    public ResponseEntity<List<EligibleClassResponseDTO>> eligibleClasses(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable Long subscriptionId) {
        return ResponseEntity.ok(
            registrationService.getEligibleClasses(principal.getUser().getUserId(), subscriptionId));
    }

    @LogAudit(actionType = "REGISTER_CLASS", targetEntity = "ClassRegistration")
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('REGISTER_CLASS')")
    @PostMapping("/class-registrations")
    public ResponseEntity<ClassRegistrationResponseDTO> register(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody ClassRegistrationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(registrationService.registerSelf(principal.getUser().getUserId(), dto.getClassId(), dto.getSubscriptionId()));
    }

    @LogAudit(actionType = "SWITCH_CLASS", targetEntity = "ClassRegistration")
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('REGISTER_CLASS')")
    @PatchMapping("/class-registrations/{registrationId}/switch")
    public ResponseEntity<ClassRegistrationResponseDTO> switchClass(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable Long registrationId,
        @Valid @RequestBody SwitchClassRequestDTO dto) {
        return ResponseEntity.ok(
            registrationService.switchSelf(principal.getUser().getUserId(), registrationId, dto.getNewClassId()));
    }

    @LogAudit(actionType = "CANCEL_REGISTRATION", targetEntity = "ClassRegistration")
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('REGISTER_CLASS')")
    @PatchMapping("/class-registrations/{registrationId}/cancel")
    public ResponseEntity<ClassRegistrationResponseDTO> cancel(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable Long registrationId) {
        return ResponseEntity.ok(
            registrationService.cancel(principal.getUser().getUserId(), registrationId));
    }

    // Lịch sử đăng ký (mọi trạng thái)
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_SCHEDULE')")
    @GetMapping("/class-registrations")
    public ResponseEntity<List<ClassRegistrationResponseDTO>> history(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(registrationService.getHistory(principal.getUser().getUserId()));
    }

    // Lịch tập sắp tới theo buổi
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_SCHEDULE')")
    @GetMapping("/schedule")
    public ResponseEntity<List<MemberSessionResponseDTO>> schedule(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(
            registrationService.getUpcomingSchedule(principal.getUser().getUserId()));
    }
}
