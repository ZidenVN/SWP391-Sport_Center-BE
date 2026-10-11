package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationResponseDTO;
import com.fptu.swp391.sportscentermanager.service.ClassRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Lễ tân đăng ký / hỗ trợ hủy lớp cho member tại quầy. */
@RestController
@RequestMapping("/api/v1/receptionist/members/{memberId}/registrations")
@RequiredArgsConstructor
public class ReceptionistRegistrationController {

    private final ClassRegistrationService registrationService;

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('REGISTER_CLASS')")
    @PostMapping
    public ResponseEntity<ClassRegistrationResponseDTO> register(
        @PathVariable Long memberId,
        @Valid @RequestBody ClassRegistrationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(registrationService.register(memberId, dto.getClassId()));
    }

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('REGISTER_CLASS')")
    @DeleteMapping("/{registrationId}")
    public ResponseEntity<ClassRegistrationResponseDTO> cancel(
        @PathVariable Long memberId,
        @PathVariable Long registrationId) {
        return ResponseEntity.ok(registrationService.cancel(memberId, registrationId));
    }

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('REGISTER_CLASS')")
    @GetMapping
    public ResponseEntity<List<ClassRegistrationResponseDTO>> history(@PathVariable Long memberId) {
        return ResponseEntity.ok(registrationService.getHistory(memberId));
    }
}
