package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.PaymentActionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.PaymentResponseDTO;
import com.fptu.swp391.sportscentermanager.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** BE-23/24: Manager đối soát giao dịch toàn trung tâm, xuất CSV và duyệt hoàn tiền. */
@RestController
@RequestMapping("/api/v1/manager/payments")
@RequiredArgsConstructor
public class ManagerPaymentController {

    private final PaymentService paymentService;

    // VD: /api/v1/manager/payments?fromDate=2026-10-01&toDate=2026-10-31&status=COMPLETED&method=CASH&keyword=0912
    @PreAuthorize("hasRole('CENTER_MANAGER') and hasAuthority('VIEW_PAYMENT')")
    @GetMapping
    public ResponseEntity<Page<PaymentResponseDTO>> getPayments(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String method,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paymentService.searchPayments(fromDate, toDate, status, method, keyword, page, size));
    }

    // Cùng bộ lọc với danh sách, trả file payments.csv
    @PreAuthorize("hasRole('CENTER_MANAGER') and hasAuthority('VIEW_PAYMENT')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPayments(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String method,
        @RequestParam(required = false) String keyword) {
        byte[] csv = paymentService.exportPaymentsCsv(fromDate, toDate, status, method, keyword);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payments.csv\"")
            .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
            .body(csv);
    }

    @LogAudit(actionType = "REFUND_PAYMENT", targetEntity = "Payment")
    @PreAuthorize("hasRole('CENTER_MANAGER') and hasAuthority('APPROVE_REFUND')")
    @PatchMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponseDTO> refund(@PathVariable Long paymentId,
                                                     @Valid @RequestBody PaymentActionRequestDTO dto) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, dto.getReason()));
    }
}
