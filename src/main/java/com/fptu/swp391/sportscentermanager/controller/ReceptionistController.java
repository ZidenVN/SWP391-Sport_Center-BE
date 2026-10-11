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
import com.fptu.swp391.sportscentermanager.dto.SubscriptionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
@RestController
@RequestMapping("/api/v1/receptionist/members")
@RequiredArgsConstructor
public class ReceptionistController {

    private final ReceptionistService receptionistService;
    private final SubscriptionService subscriptionService;
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

    // ---------- Manage Member Subscriptions (include Process Payment) ----------
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('MANAGE_SUBSCRIPTION')")
    @PostMapping("/{memberId}/subscriptions")
    public ResponseEntity<SubscriptionResponseDTO> subscribeForMember(
        @PathVariable Long memberId,
        @Valid @RequestBody SubscriptionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(subscriptionService.subscribe(memberId, dto));
    }

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('MANAGE_SUBSCRIPTION')")
    @GetMapping("/{memberId}/subscriptions")
    public ResponseEntity<List<SubscriptionResponseDTO>> getMemberSubscriptions(
        @PathVariable Long memberId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptions(memberId));
    }

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('MANAGE_SUBSCRIPTION')")
    @GetMapping("/{memberId}/subscriptions/current")
    public ResponseEntity<SubscriptionResponseDTO> getMemberCurrentSubscription(
        @PathVariable Long memberId) {
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(memberId));
    }
}
