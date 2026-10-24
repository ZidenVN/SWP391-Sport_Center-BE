package com.fptu.swp391.sportscentermanager.controller;


import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.CheckInResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.CheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/receptionist")
@RequiredArgsConstructor
public class CheckInController {
    private final CheckInService checkInService;

    @LogAudit(actionType = "CHECK_IN", targetEntity = "Member")
    @PreAuthorize("hasRole('RECEPTIONIST') AND hasAuthority('CHECK_IN_MEMBER')")
    @PostMapping("/members/{memberId}/check-ins")
    public ResponseEntity<CheckInResponseDTO> checkIn(@PathVariable Long memberId, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(checkInService.checkIn    (memberId, principal.getUser().getUserId()));
    }

    @PreAuthorize("hasRole('RECEPTIONIST') AND hasAuthority('CHECK_IN_MEMBER')")
    @GetMapping("/check-ins")
    public ResponseEntity<List<CheckInResponseDTO>> checkIns(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date) {
        return ResponseEntity.ok(checkInService.getCheckIns(date != null ? date : LocalDate.now()));
    }

}
