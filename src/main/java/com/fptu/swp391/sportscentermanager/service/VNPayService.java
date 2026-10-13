package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.VNPayCallBackResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.VNPayPaymentResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface VNPayService {
    VNPayPaymentResponseDTO createPaymentUrl(Long memberId, Long packageId, HttpServletRequest request);

    VNPayCallBackResponseDTO processCallBack(HttpServletRequest request);
}
