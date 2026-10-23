package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.InvoiceResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.PaymentResponseDTO;

import java.util.List;
import com.fptu.swp391.sportscentermanager.dto.ConfirmPaymentRequestDTO;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
public interface PaymentService {

    List<PaymentResponseDTO> getMemberPayments(Long memberId);

    /**
     * Dựng hóa đơn.
     * @param ownerMemberId nếu khác null: chỉ cho xem khi giao dịch thuộc member này (Member tự xem).
     *                      nếu null: không kiểm tra chủ sở hữu (Lễ tân).
     */
    InvoiceResponseDTO getInvoice(Long paymentId, Long ownerMemberId);

    // BE-18: lễ tân xác nhận đã thu tiền tại quầy -> COMPLETED + kích hoạt gói
    PaymentResponseDTO confirmCounterPayment(Long paymentId, Long cashierId, ConfirmPaymentRequestDTO dto);

    // BE-18: hủy giao dịch đang chờ (khách không trả tiền nữa)
    PaymentResponseDTO cancelPendingPayment(Long paymentId, String reason);

    // BE-24: Manager duyệt hoàn tiền giao dịch đã thu
    PaymentResponseDTO refundPayment(Long paymentId, String reason);

    // BE-23: danh sách giao dịch toàn trung tâm
    Page<PaymentResponseDTO> searchPayments(LocalDate fromDate, LocalDate toDate, String status, String method,
                                            String keyword, int page, int size);

    // BE-24: xuất CSV theo đúng bộ lọc của danh sách
    byte[] exportPaymentsCsv(LocalDate fromDate, LocalDate toDate, String status, String method, String keyword);
}
