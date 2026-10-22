package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.ConfirmPaymentRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.InvoiceResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.PaymentActionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.PaymentResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Lễ tân xem thanh toán, xác nhận thu tiền và in/xuất hóa đơn.
 * Luồng tại quầy (BE-17/18):
 * 1. POST /api/v1/receptionist/members/{memberId}/subscriptions -> tạo gói PENDING + giao dịch PENDING
 * 2. PATCH /api/v1/receptionist/payments/{paymentId}/confirm   -> đã thu tiền: COMPLETED + kích hoạt gói
 *    (hoặc PATCH .../cancel nếu khách không trả nữa)
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

    // BE-18/FE-25: danh sách giao dịch tại quầy, vd ?status=PENDING để xem các khoản chờ thu
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('VIEW_PAYMENT')")
    @GetMapping("/payments")
    public ResponseEntity<Page<PaymentResponseDTO>> payments(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String method,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paymentService.searchPayments(fromDate, toDate, status, method, keyword, page, size));
    }

    // BE-18: xác nhận đã thu tiền tại quầy
    @LogAudit(actionType = "CONFIRM_PAYMENT", targetEntity = "Payment")
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('CREATE_PAYMENT')")
    @PatchMapping("/payments/{paymentId}/confirm")
    public ResponseEntity<PaymentResponseDTO> confirm(@AuthenticationPrincipal CustomUserDetails principal,
                                                      @PathVariable Long paymentId,
                                                      @Valid @RequestBody(required = false) ConfirmPaymentRequestDTO dto) {
        return ResponseEntity.ok(paymentService.confirmCounterPayment(paymentId, principal.getUser().getUserId(), dto));
    }

    // BE-18: hủy giao dịch đang chờ thu
    @LogAudit(actionType = "CANCEL_PAYMENT", targetEntity = "Payment")
    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('CREATE_PAYMENT')")
    @PatchMapping("/payments/{paymentId}/cancel")
    public ResponseEntity<PaymentResponseDTO> cancel(@PathVariable Long paymentId,
                                                     @Valid @RequestBody PaymentActionRequestDTO dto) {
        return ResponseEntity.ok(paymentService.cancelPendingPayment(paymentId, dto.getReason()));
    }

    @PreAuthorize("hasRole('RECEPTIONIST') and hasAuthority('VIEW_PAYMENT')")
    @GetMapping("/payments/{paymentId}/invoice")
    public ResponseEntity<InvoiceResponseDTO> invoice(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getInvoice(paymentId, null));
    }
}
