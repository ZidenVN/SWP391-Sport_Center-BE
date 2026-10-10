package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.MemberSearchResponseDTO;
import com.fptu.swp391.sportscentermanager.service.ReceptionistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/receptionist/members")
@RequiredArgsConstructor
public class ReceptionistController {

    private final ReceptionistService receptionistService;

    // ---------- Search Member Info ----------
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('SEARCH_MEMBER')")
    @GetMapping
    public ResponseEntity<List<MemberSearchResponseDTO>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(receptionistService.searchMembers(keyword));
    }

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('SEARCH_MEMBER')")
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberSearchResponseDTO> getMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(receptionistService.getMember(memberId));
    }
}
