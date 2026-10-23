package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter @Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;
    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    // Thời điểm thực thu tiền (chỉ có khi COMPLETED). Doanh thu tính theo cột này.
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Mã giao dịch ngân hàng / VNPay để đối soát
    @Column(name = "transaction_ref")
    private String transactionRef;

    // Lễ tân xác nhận thu tiền (null nếu thanh toán online)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    // Ghi chú: lý do hủy / hoàn tiền
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
