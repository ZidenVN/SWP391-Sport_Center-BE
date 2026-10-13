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
}
