package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.*;
import com.fptu.swp391.sportscentermanager.service.MemberService;
import com.fptu.swp391.sportscentermanager.service.ReceptionistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
@RestController
@RequestMapping("/api/v1/receptionist/members")
@RequiredArgsConstructor
public class ReceptionistController {

    private final ReceptionistService receptionistService;
    private final SubscriptionService subscriptionService;
    private final MemberService memberService;
    // ---------- Search Member Info ----------
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('SEARCH_MEMBER')")
    @GetMapping
    public ResponseEntity<List<MemberSearchResponseDTO>> search(@RequestParam(defaultValue = "") String keyword) {
        return ResponseEntity.ok(receptionistService.searchMembers(keyword));
    }

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('SEARCH_MEMBER')")
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberSearchResponseDTO> getMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(receptionistService.getMember(memberId));
    }

    @LogAudit(actionType = "UPDATE", targetEntity = "Member")
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('UPDATE_USER')")
    @PutMapping("/{memberId}")
    public ResponseEntity<MemberProfileResponseDTO> updateMember(@PathVariable Long memberId, @Valid @RequestBody MemberProfileUpdateDTO dto){
        return ResponseEntity.ok(memberService.updateProfile(memberId, dto));
    }


    // ---------- Register Walk-in Member ----------
    @LogAudit(actionType = "CREATE", targetEntity = "Member")
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('CREATE_USER')")
    @PostMapping
    public ResponseEntity<WalkInMemberResponseDTO> registerWalkIn(
        @Valid @RequestBody WalkInMemberRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(receptionistService.registerWalkInMember(dto));
    }

    // ---------- Manage Member Subscriptions (include Process Payment) ----------
    // BE-17: tạo gói + giao dịch ở trạng thái PENDING. Thu tiền xong gọi
    // PATCH /api/v1/receptionist/payments/{paymentId}/confirm để kích hoạt gói.
    @LogAudit(actionType = "SUBSCRIBE_PACKAGE", targetEntity = "Member")
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
