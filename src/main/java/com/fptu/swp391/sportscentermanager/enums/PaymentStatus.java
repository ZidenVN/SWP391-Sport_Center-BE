package com.fptu.swp391.sportscentermanager.enums;

/**
 * Vòng đời của 1 giao dịch (BE-17).
 * PENDING  -> COMPLETED (đã thu tiền, chỉ trạng thái này mới tính doanh thu)
 * PENDING  -> FAILED / CANCELLED
 * COMPLETED -> REFUNDED (Manager duyệt hoàn tiền)
 */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REFUNDED
}
