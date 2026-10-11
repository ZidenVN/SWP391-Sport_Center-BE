package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.InvoiceResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.PaymentResponseDTO;

import java.util.List;

public interface PaymentService {

    List<PaymentResponseDTO> getMemberPayments(Long memberId);

    /**
     * Dựng hóa đơn.
     * @param ownerMemberId nếu khác null: chỉ cho xem khi giao dịch thuộc member này (Member tự xem).
     *                      nếu null: không kiểm tra chủ sở hữu (Lễ tân).
     */
    InvoiceResponseDTO getInvoice(Long paymentId, Long ownerMemberId);
}
