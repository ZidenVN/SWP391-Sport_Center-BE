package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.InvoiceResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.PaymentResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.entity.Payment;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.PaymentRepository;
import com.fptu.swp391.sportscentermanager.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String PACKAGE_UNAVAILABLE = "(Gói đã ngừng bán)";

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;

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

        MembershipPackage pkg = sub.getMembershipPackage();
        return InvoiceResponseDTO.builder()
            .invoiceNumber(String.format("INV-%06d", payment.getPaymentId()))
            .paymentId(payment.getPaymentId())
            .issuedAt(payment.getPaymentDate())
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
            .build();
    }

    private PaymentResponseDTO toResponse(Payment p) {
        Subscription s = p.getSubscription();
        MembershipPackage pkg = s.getMembershipPackage();
        return PaymentResponseDTO.builder()
            .paymentId(p.getPaymentId())
            .subscriptionId(s.getSubscriptionId())
            .packageName(pkg != null ? pkg.getPackageName() : PACKAGE_UNAVAILABLE)
            .amount(p.getAmount())
            .paymentMethod(p.getPaymentMethod())
            .paymentDate(p.getPaymentDate())
            .status(p.getStatus())
            .build();
    }
}
