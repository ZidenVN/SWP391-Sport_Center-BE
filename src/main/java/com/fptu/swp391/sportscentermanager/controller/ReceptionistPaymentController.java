package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.InvoiceResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.PaymentResponseDTO;
import com.fptu.swp391.sportscentermanager.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lễ tân xem thanh toán và in/xuất hóa đơn.
 * Việc GHI NHẬN thanh toán xảy ra khi bán/gia hạn gói:
 * POST /api/v1/receptionist/members/{memberId}/subscriptions (Process Payment là include của use case đó).
 */
@RestController
@RequestMapping("/api/v1/receptionist")
@RequiredArgsConstructor
public class ReceptionistPaymentController {

    private final PaymentService paymentService;

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('VIEW_PAYMENT')")
    @GetMapping("/members/{memberId}/payments")
    public ResponseEntity<List<PaymentResponseDTO>> memberPayments(@PathVariable Long memberId) {
        return ResponseEntity.ok(paymentService.getMemberPayments(memberId));
    }

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('VIEW_PAYMENT')")
    @GetMapping("/payments/{paymentId}/invoice")
    public ResponseEntity<InvoiceResponseDTO> invoice(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getInvoice(paymentId, null));
    }
}
