package com.fptu.swp391.sportscentermanager.controller;


import com.fptu.swp391.sportscentermanager.dto.SubscriptionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members/me/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PreAuthorize("hasRole('MEMBER') and hasAuthority('SUBSCRIBE_PACKAGE')")
    @PostMapping
    public ResponseEntity<SubscriptionResponseDTO> subscribe(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody SubscriptionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(subscriptionService.subscribe(principal.getUser().getUserId(), dto));
    }

    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_PROFILE')")
    @GetMapping
    public ResponseEntity<List<SubscriptionResponseDTO>> getMySubscriptions(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(subscriptionService.getSubscriptions(principal.getUser().getUserId()));
    }

    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_PROFILE')")
    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponseDTO> getMyCurrentSubscription(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(principal.getUser().getUserId()));
    }

}
