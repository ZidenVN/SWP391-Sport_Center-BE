package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.InvoiceResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.PaymentResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Member xem lịch sử thanh toán và hóa đơn của chính mình. */
@RestController
@RequestMapping("/api/v1/members/me/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_PROFILE')")
    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> myPayments(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(paymentService.getMemberPayments(principal.getUser().getUserId()));
    }

    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_PROFILE')")
    @GetMapping("/{paymentId}/invoice")
    public ResponseEntity<InvoiceResponseDTO> myInvoice(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable Long paymentId) {
        return ResponseEntity.ok(
            paymentService.getInvoice(paymentId, principal.getUser().getUserId()));
    }
}
