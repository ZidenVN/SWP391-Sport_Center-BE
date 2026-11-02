package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.VNPayCallBackResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.VNPayPaymentResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments/vnpay")
public class VNPayController {
    private final VNPayService vnPayService;

    @PreAuthorize("hasRole('MEMBER') AND hasAuthority('SUBSCRIBE_PACKAGE')")
    @PostMapping("/create-url")
    public ResponseEntity<VNPayPaymentResponseDTO> createPaymentUrl(@AuthenticationPrincipal CustomUserDetails principal, @RequestParam Long packageId, HttpServletRequest request) {
        return ResponseEntity.ok(vnPayService.createPaymentUrl(principal.getUser().getUserId(), packageId, request));
    }

    @GetMapping("/callback")
    public ResponseEntity<VNPayCallBackResponseDTO> processCallBack(HttpServletRequest request) {
        return ResponseEntity.ok(vnPayService.processCallBack(request));
    }

    // IPN: server VNPay gọi thẳng về, không phụ thuộc trình duyệt khách; VNPay chỉ đọc RspCode + Message
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> ipn(HttpServletRequest request) {
        VNPayCallBackResponseDTO r = vnPayService.processCallBack(request);
        return ResponseEntity.ok(Map.of("RspCode", r.getRspCode(), "Message", r.getMessage()));
    }
}
