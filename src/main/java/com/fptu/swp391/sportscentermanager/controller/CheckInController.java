package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.CheckInRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.CheckInResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.CheckInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Điểm danh hội viên khi đến trung tâm (check-in tại quầy).
 * Lễ tân: ghi nhận + xem danh sách theo ngày / lịch sử của 1 hội viên. Hội viên: xem lượt đến của mình.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @LogAudit(actionType = "CHECK_IN", targetEntity = "Member")
    @PreAuthorize("hasAuthority('CHECK_IN_MEMBER')")
    @PostMapping("/receptionist/check-ins")
    public ResponseEntity<CheckInResponseDTO> checkIn(@AuthenticationPrincipal CustomUserDetails principal,
                                                      @Valid @RequestBody CheckInRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(checkInService.checkIn(dto.getMemberId(), principal.getUser().getUserId(), dto.getNote()));
    }

    // VD: /api/v1/receptionist/check-ins?date=2026-10-23 (mặc định hôm nay)
    @PreAuthorize("hasAuthority('CHECK_IN_MEMBER')")
    @GetMapping("/receptionist/check-ins")
    public ResponseEntity<List<CheckInResponseDTO>> byDate(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(checkInService.getByDate(date));
    }

    @PreAuthorize("hasAuthority('CHECK_IN_MEMBER')")
    @GetMapping("/receptionist/members/{memberId}/check-ins")
    public ResponseEntity<List<CheckInResponseDTO>> memberHistory(@PathVariable Long memberId) {
        return ResponseEntity.ok(checkInService.getMemberHistory(memberId));
    }

    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_SCHEDULE')")
    @GetMapping("/members/me/check-ins")
    public ResponseEntity<List<CheckInResponseDTO>> myCheckIns(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(checkInService.getMemberHistory(principal.getUser().getUserId()));
    }
}
