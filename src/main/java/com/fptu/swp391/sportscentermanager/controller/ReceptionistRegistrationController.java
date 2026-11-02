package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.EligibleClassResponseDTO;
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

/** Lễ tân đăng ký / đổi / hỗ trợ hủy lớp cho member tại quầy. */
@RestController
@RequestMapping("/api/v1/receptionist")
@RequiredArgsConstructor
public class ReceptionistRegistrationController {

    private final ClassRegistrationService registrationService;

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('REGISTER_CLASS')")
    @GetMapping("/members/{memberId}/subscriptions/{subscriptionId}/eligible-classes")
    public ResponseEntity<List<EligibleClassResponseDTO>> eligibleClasses(
        @PathVariable Long memberId,
        @PathVariable Long subscriptionId) {
        return ResponseEntity.ok(registrationService.getEligibleClasses(memberId, subscriptionId));
    }

    @LogAudit(actionType = "REGISTER_CLASS", targetEntity = "ClassRegistration")
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('REGISTER_CLASS')")
    @PostMapping("/members/{memberId}/class-registrations")
    public ResponseEntity<ClassRegistrationResponseDTO> register(
        @PathVariable Long memberId,
        @Valid @RequestBody ClassRegistrationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(registrationService.register(memberId, dto.getClassId(), dto.getSubscriptionId()));
    }

    @LogAudit(actionType = "CANCEL_REGISTRATION", targetEntity = "ClassRegistration")
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('REGISTER_CLASS')")
    @PatchMapping("/members/{memberId}/class-registrations/{registrationId}/cancel")
    public ResponseEntity<ClassRegistrationResponseDTO> cancel(
        @PathVariable Long memberId,
        @PathVariable Long registrationId) {
        return ResponseEntity.ok(registrationService.cancel(memberId, registrationId));
    }

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('REGISTER_CLASS')")
    @GetMapping("/members/{memberId}/class-registrations")
    public ResponseEntity<List<ClassRegistrationResponseDTO>> history(@PathVariable Long memberId) {
        return ResponseEntity.ok(registrationService.getHistory(memberId));
    }

    // Lễ tân đổi lớp: tính 1 lượt / gói lớp khi hội viên đã hết quyền tự đổi; đăng ký của lớp bị dừng thì không tính lượt
    @LogAudit(actionType = "SWITCH_CLASS", targetEntity = "ClassRegistration")
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('SWITCH_CLASS_REGISTRATION')")
    @PatchMapping("/class-registrations/{registrationId}/switch")
    public ResponseEntity<ClassRegistrationResponseDTO> switchClass(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable Long registrationId,
        @Valid @RequestBody SwitchClassRequestDTO dto) {
        return ResponseEntity.ok(registrationService.switchByReceptionist(
            principal.getUser().getUserId(), registrationId, dto.getNewClassId()));
    }
}
