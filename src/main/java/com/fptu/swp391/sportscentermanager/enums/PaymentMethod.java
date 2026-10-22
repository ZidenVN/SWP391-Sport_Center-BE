package com.fptu.swp391.sportscentermanager.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Phương thức thanh toán (BE-19).
 * - Thu tại quầy (CASH, BANK_TRANSFER, CARD, E_WALLET): tạo PENDING, lễ tân bấm xác nhận khi đã nhận tiền.
 * - Online (VNPAY): chỉ được chuyển COMPLETED qua callback có chữ ký của cổng thanh toán.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CASH(false, false),
    BANK_TRANSFER(false, true),
    CARD(false, false),
    E_WALLET(false, false),
    VNPAY(true, true);

    private final boolean online;
    // Bắt buộc có mã giao dịch để đối soát
    private final boolean transactionRefRequired;

    public static PaymentMethod from(String value) {
        for (PaymentMethod m : values()) {
            if (m.name().equalsIgnoreCase(value)) return m;
        }
        return null;
    }
}
