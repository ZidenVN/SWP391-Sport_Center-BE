package com.fptu.swp391.sportscentermanager.controller;


import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.fptu.swp391.sportscentermanager.dto.PurchasePreviewResponseDTO;
@RestController
@RequestMapping("/api/v1/members/me/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_PROFILE')")
    @GetMapping
    public ResponseEntity<List<SubscriptionResponseDTO>> getMySubscriptions(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(subscriptionService.getSubscriptions(principal.getUser().getUserId()));
    }

    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_PROFILE')")
    @GetMapping("/current")
    public ResponseEntity<List<SubscriptionResponseDTO>> getMyCurrentSubscriptions(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(subscriptionService.getCurrentSubscriptions(principal.getUser().getUserId()));
    }

    // BR-SUB-08: FE gọi trước khi thanh toán để hiện ngày dự kiến + cảnh báo gói thừa
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('SUBSCRIBE_PACKAGE')")
    @GetMapping("/preview")
    public ResponseEntity<PurchasePreviewResponseDTO> previewMyPurchase(
        @AuthenticationPrincipal CustomUserDetails principal,
        @RequestParam Long packageId) {
        return ResponseEntity.ok(subscriptionService.previewPurchase(principal.getUser().getUserId(), packageId));
    }

}
