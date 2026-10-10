package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.MemberSearchResponseDTO;
import com.fptu.swp391.sportscentermanager.service.ReceptionistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.fptu.swp391.sportscentermanager.dto.WalkInMemberRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.WalkInMemberResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    // ---------- Register Walk-in Member ----------
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('CREATE_USER')")
    @PostMapping
    public ResponseEntity<WalkInMemberResponseDTO> registerWalkIn(
        @Valid @RequestBody WalkInMemberRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(receptionistService.registerWalkInMember(dto));
    }
}
