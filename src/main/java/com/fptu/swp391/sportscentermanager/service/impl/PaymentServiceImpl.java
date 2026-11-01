package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.InvoiceResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.PaymentResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.*;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;

import com.fptu.swp391.sportscentermanager.dto.ConfirmPaymentRequestDTO;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String PACKAGE_UNAVAILABLE = "(Gói đã ngừng bán)";

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;

    private static final int MAX_PAGE_SIZE = 100;
    // 'SUCCESS' là dữ liệu cũ, coi như đã thu
    private static final Set<String> PAID_STATUSES = Set.of(PaymentStatus.COMPLETED.name(), "SUCCESS");
    // Hóa đơn chỉ xuất cho giao dịch đã thu (kể cả đã hoàn tiền sau đó, để đối chiếu)
    private static final Set<String> INVOICEABLE_STATUSES = Set.of(PaymentStatus.COMPLETED.name(), "SUCCESS",
        PaymentStatus.REFUNDED.name());
    private final MemberRepository memberRepository;
    private final ClassSessionRepository classSessionRepository;

    @Override
    @Transactional
    public List<PaymentResponseDTO> getMemberPayments(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return paymentRepository.findByMemberId(memberId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public InvoiceResponseDTO getInvoice(Long paymentId, Long ownerMemberId) {
        Payment payment = paymentRepository.findDetailById(paymentId)
            .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        Subscription sub = payment.getSubscription();
        Member member = sub.getMember();

        // Member chỉ xem được hóa đơn của chính mình. Báo "không tồn tại" để không lộ ID của người khác.
        if (ownerMemberId != null && !ownerMemberId.equals(member.getUserId())) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        // BE-20: không xuất hóa đơn cho giao dịch chưa thu / thất bại / đã hủy
        if (!INVOICEABLE_STATUSES.contains(payment.getStatus())) {
            throw new AppException(ErrorCode.INVOICE_NOT_AVAILABLE);
        }

        MembershipPackage pkg = sub.getMembershipPackage();
        return InvoiceResponseDTO.builder()
            .invoiceNumber(String.format("INV-%06d", payment.getPaymentId()))
            .paymentId(payment.getPaymentId())
            .issuedAt(payment.getPaidAt() != null ? payment.getPaidAt() : payment.getPaymentDate())
            .memberId(member.getUserId())
            .memberName(member.getLastName() + " " + member.getFirstName())
            .memberEmail(member.getEmail())
            .memberPhone(member.getPhone())
            .subscriptionId(sub.getSubscriptionId())
            .packageName(pkg != null ? pkg.getPackageName() : PACKAGE_UNAVAILABLE)
            .durationDays(pkg != null ? pkg.getDurationDays() : null)
            .startDate(sub.getStartDate())
            .endDate(sub.getEndDate())
            .amount(payment.getAmount())
            .paymentMethod(payment.getPaymentMethod())
            .paymentStatus(payment.getStatus())
            .paidAt(payment.getPaidAt())
            .transactionRef(payment.getTransactionRef())
            .cashierName(fullName(payment.getConfirmedBy()))
            .build();
    }

    @Override
    @Transactional
    public PaymentResponseDTO confirmCounterPayment(Long paymentId, Long cashierId, ConfirmPaymentRequestDTO dto) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        // Chống xác nhận 2 lần
        if (!PaymentStatus.PENDING.name().equals(payment.getStatus())) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        PaymentMethod method = PaymentMethod.from(payment.getPaymentMethod());
        // VNPay chỉ được xác nhận qua callback có chữ ký, lễ tân không tự bấm được
        if (method == null || method.isOnline()) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_ALLOWED);
        }
        String ref = dto != null && dto.getTransactionRef() != null ? dto.getTransactionRef().trim() : null;
        if (method.isTransactionRefRequired() && (ref == null || ref.isEmpty())) {
            throw new AppException(ErrorCode.TRANSACTION_REF_REQUIRED);
        }

        User cashier = userRepository.findById(cashierId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(PaymentStatus.COMPLETED.name());
        payment.setPaidAt(now);
        payment.setConfirmedBy(cashier);
        payment.setTransactionRef(ref == null || ref.isEmpty() ? null : ref);
        if (dto != null && dto.getNote() != null) {
            payment.setNote(dto.getNote());
        }
        paymentRepository.save(payment);

        // Thu tiền xong mới kích hoạt gói
        subscriptionService.markPaid(payment.getSubscription(), now);

        return toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDTO cancelPendingPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        if (!PaymentStatus.PENDING.name().equals(payment.getStatus())) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        payment.setStatus(PaymentStatus.CANCELLED.name());
        payment.setNote(reason);
        paymentRepository.save(payment);

        Subscription sub = payment.getSubscription();
        sub.setStatus("CANCELLED");
        subscriptionRepository.save(sub);

        return toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDTO refundPayment(Long paymentId, String reason) {

        Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        if (!PAID_STATUSES.contains(payment.getStatus())) {
            throw new AppException(ErrorCode.REFUND_NOT_ALLOWED);
        }

        LocalDateTime now = LocalDateTime.now();
        Subscription sub = payment.getSubscription();
        Optional<ClassRegistration> registration = sub != null ? subscriptionRepository.findLiveRegistration(sub.getSubscriptionId()) : Optional.empty();

        // ko hoan tien dc thi nem loi ngay
        BigDecimal refundAmount = calculateRefundAmount(payment, sub, registration, now);
        payment.setStatus(PaymentStatus.REFUNDED.name());
        payment.setRefundAmount(refundAmount);
        payment.setNote(reason);
        paymentRepository.save(payment);

        if (sub != null) {
            sub.setStatus(SubscriptionStatus.REFUNDED.name());
            subscriptionRepository.save(sub);
            registration.ifPresent(r -> {
                r.setStatus("CANCELLED");
                r.setCancelledAt(now);
            });

            notifyRefund(sub, refundAmount, reason);
        }
        return toResponse(payment);
    }

    @Override
    @Transactional
    public Page<PaymentResponseDTO> searchPayments(LocalDate fromDate, LocalDate toDate, String status, String method,
                                                   String keyword, int page, int size) {
        LocalDateTime[] range = toRange(fromDate, toDate);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return paymentRepository.search(blankToNull(status), blankToNull(method), range[0], range[1],
                blankToNull(keyword), pageable)
            .map(this::toResponse);
    }

    @Override
    @Transactional
    public byte[] exportPaymentsCsv(LocalDate fromDate, LocalDate toDate, String status, String method, String keyword) {
        LocalDateTime[] range = toRange(fromDate, toDate);
        List<Payment> payments = paymentRepository.searchAll(blankToNull(status), blankToNull(method),
            range[0], range[1], blankToNull(keyword));

        StringBuilder csv = new StringBuilder();
        // BOM để Excel đọc đúng tiếng Việt
        csv.append('﻿');
        csv.append("Mã GD,Hóa đơn,Hội viên,Email,Gói tập,Số tiền,Phương thức,Trạng thái,Ngày tạo,Ngày thu,Mã đối soát,Người xác nhận,Ghi chú\n");
        for (Payment p : payments) {
            PaymentResponseDTO r = toResponse(p);
            csv.append(r.getPaymentId()).append(',')
                .append(String.format("INV-%06d", r.getPaymentId())).append(',')
                .append(csvCell(r.getMemberName())).append(',')
                .append(csvCell(p.getSubscription().getMember().getEmail())).append(',')
                .append(csvCell(r.getPackageName())).append(',')
                .append(r.getAmount() != null ? r.getAmount().toPlainString() : "").append(',')
                .append(csvCell(r.getPaymentMethod())).append(',')
                .append(csvCell(r.getStatus())).append(',')
                .append(r.getPaymentDate() != null ? r.getPaymentDate() : "").append(',')
                .append(r.getPaidAt() != null ? r.getPaidAt() : "").append(',')
                .append(csvCell(r.getTransactionRef())).append(',')
                .append(csvCell(r.getConfirmedBy())).append(',')
                .append(csvCell(r.getNote())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private LocalDateTime[] toRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
        return new LocalDateTime[]{
            fromDate != null ? fromDate.atStartOfDay() : null,
            toDate != null ? toDate.atTime(LocalTime.MAX) : null
        };
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String csvCell(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String fullName(User u) {
        return u != null ? u.getLastName() + " " + u.getFirstName() : null;
    }

    private PaymentResponseDTO toResponse(Payment p) {
        Subscription s = p.getSubscription();
        MembershipPackage pkg = s.getMembershipPackage();
        Member m = s.getMember();
        return PaymentResponseDTO.builder()
            .paymentId(p.getPaymentId())
            .subscriptionId(s.getSubscriptionId())
            .packageName(pkg != null ? pkg.getPackageName() : PACKAGE_UNAVAILABLE)
            .amount(p.getAmount())
            .paymentMethod(p.getPaymentMethod())
            .paymentDate(p.getPaymentDate())
            .status(p.getStatus())
            .memberId(m.getUserId())
            .memberName(fullName(m))
            .paidAt(p.getPaidAt())
            .transactionRef(p.getTransactionRef())
            .confirmedBy(fullName(p.getConfirmedBy()))
            .note(p.getNote())
            .build();
    }

    private BigDecimal calculateRefundAmount(Payment payment, Subscription sub, Optional<ClassRegistration> registration, LocalDateTime now) {
         BigDecimal paid = payment.getAmount();
         if (sub == null || SubscriptionStatus.WAITING_ACTIVATION.name().equals(sub.getStatus())) {
             return paid;
         }

         boolean activeClassPackage = SubscriptionStatus.ACTIVE.name().equals(sub.getStatus()) && sub.getMembershipPackage().getPackageType() == PackageType.CLASS;
         if (!activeClassPackage || registration.isEmpty()) {
             throw new AppException(ErrorCode.REFUND_PACKAGE_ALREADY_USED);
         }

         SportClass sportClass = registration.get().getSportClass();
         if ("STOPPED".equals(sportClass.getStatus())) {
             return proratedForStoppedClass(paid, sportClass.getClassId());
         }

         if (sportClass.getFirstSessionAt() != null && sportClass.getFirstSessionAt().isAfter(now)){
             return paid;
         }

         throw new AppException(ErrorCode.REFUND_PACKAGE_ALREADY_USED);
    }

    // lớp dừng giữa khoá = (giá tiền x buổi chưa học) / tổng buổi

    private BigDecimal proratedForStoppedClass(BigDecimal paid, Long classId) {
        long total = classSessionRepository.countBySportClass_ClassId(classId) - classSessionRepository.countBySportClass_ClassIdAndStatus(classId, SessionStatus.POSTPONED);
        long notStudied = classSessionRepository.countBySportClass_ClassIdAndStatus(classId, SessionStatus.CANCELLED);
        if (total <= 0){
            return paid;
        }
        return paid.multiply(BigDecimal.valueOf(notStudied)).divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP);
    }

    private void notifyRefund(Subscription sub, BigDecimal amount, String reason){
        String money = NumberFormat.getInstance(Locale.of("vi", "VN")).format(amount) + "đ";
        StringBuilder content = new StringBuilder(sub.getMembershipPackage().getPackageName() + " của bạn đã được hoàn " + money + " và hết hiệu lực.");
        if(reason != null && !reason.isBlank()){
            content.append(" Lý do: ").append(reason.trim()).append(".");
        }
        notificationService.notifyUsers(List.of(sub.getMember()), "Gói tập đã được hoàn tiền", content.toString(), "SUBSCRIPTION_REFUNDED");
    }
}
