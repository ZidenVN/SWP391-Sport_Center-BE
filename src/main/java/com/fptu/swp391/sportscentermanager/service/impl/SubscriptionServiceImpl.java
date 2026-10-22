package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.SubscriptionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.entity.Payment;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.PaymentMethod;
import com.fptu.swp391.sportscentermanager.enums.PaymentStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.MembershipPackageRepository;
import com.fptu.swp391.sportscentermanager.repository.PaymentRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final MembershipPackageRepository packageRepository;

    @Override
    @Transactional
    public SubscriptionResponseDTO subscribe(Long memberId, SubscriptionRequestDTO dto) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        MembershipPackage pkg = packageRepository.findById(dto.getPackageId())
            .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        // BE-17/D-10: không cho mở thêm giao dịch khi member còn 1 giao dịch đang chờ thu tiền
        for (Subscription pending : subscriptionRepository.findByMember_UserIdAndStatus(memberId, "PENDING")) {
            Payment pendingPayment = latestPayment(pending);
            if (pendingPayment == null || !PaymentStatus.PENDING.name().equals(pendingPayment.getStatus())) continue;
            boolean staleOnline = PaymentMethod.VNPAY.name().equals(pendingPayment.getPaymentMethod())
                && pendingPayment.getPaymentDate().isBefore(now.minusMinutes(15));
            if (!staleOnline) {
                throw new AppException(ErrorCode.PAYMENT_PENDING_EXISTS);
            }
            // Link VNPay đã hết hạn: hủy để không treo mãi
            pending.setStatus("CANCELLED");
            pendingPayment.setStatus(PaymentStatus.CANCELLED.name());
        }

        // Ngày dự kiến (tính lại lúc xác nhận thu tiền, xem activate)
        LocalDateTime startDate = calculateStartDate(memberId, now);
        LocalDateTime endDate = startDate.plusDays(pkg.getDurationDays());

        // Gói chỉ ACTIVE sau khi lễ tân xác nhận đã thu tiền: PATCH /api/v1/receptionist/payments/{paymentId}/confirm
        Subscription subscription = subscriptionRepository.save(Subscription.builder()
            .member(member)
            .membershipPackage(pkg)
            .startDate(startDate)
            .endDate(endDate)
            .status("PENDING")
            .build());

        // Process Payment (include): ghi nhận giao dịch, chưa phải thực thu
        Payment payment = paymentRepository.save(Payment.builder()
            .subscription(subscription)
            .amount(BigDecimal.valueOf(pkg.getPrice()))
            .paymentMethod(dto.getPaymentMethod())
            .paymentDate(now)
            .status(PaymentStatus.PENDING.name())
            .build());

        return toResponse(subscription, payment);
    }

    @Override
    @Transactional
    public void activate(Subscription subscription, LocalDateTime paidAt) {
        LocalDateTime startDate = calculateStartDate(subscription.getMember().getUserId(), paidAt);
        subscription.setStartDate(startDate);
        subscription.setEndDate(startDate.plusDays(subscription.getMembershipPackage().getDurationDays()));
        subscription.setStatus("ACTIVE");
        subscriptionRepository.save(subscription);
    }

    // Gia hạn: nếu đang có gói còn hạn thì nối tiếp, không thì bắt đầu từ bây giờ
    private LocalDateTime calculateStartDate(Long memberId, LocalDateTime now) {
        return subscriptionRepository
            .findFirstByMember_UserIdAndStatusOrderByEndDateDesc(memberId, "ACTIVE")
            .map(Subscription::getEndDate)
            .filter(d -> d.isAfter(now))
            .orElse(now);
    }

    @Override
    @Transactional
    public List<SubscriptionResponseDTO> getSubscriptions(Long memberId) {
        return subscriptionRepository.findByMember_UserIdOrderByEndDateDesc(memberId)
            .stream()
            .map(s -> toResponse(s, latestPayment(s)))
            .toList();
    }

    @Override
    @Transactional
    public SubscriptionResponseDTO getCurrentSubscription(Long memberId) {
        return subscriptionRepository.findCurrent(memberId, LocalDateTime.now())
            .stream()
            .findFirst()
            .map(s -> toResponse(s, latestPayment(s)))
            .orElseThrow(() -> new AppException(ErrorCode.NO_ACTIVE_SUBSCRIPTION));
    }

    // Lấy giao dịch gần nhất của gói để hiển thị amount / paymentMethod / paymentStatus
    private Payment latestPayment(Subscription s) {
        return paymentRepository
            .findFirstBySubscription_SubscriptionIdOrderByPaymentDateDesc(s.getSubscriptionId())
            .orElse(null);
    }

    private SubscriptionResponseDTO toResponse(Subscription s, Payment p) {
        SubscriptionResponseDTO dto = new SubscriptionResponseDTO();
        dto.setSubscriptionId(s.getSubscriptionId());
        dto.setPackageId(s.getMembershipPackage().getPackageId());
        dto.setPackageName(s.getMembershipPackage().getPackageName());
        dto.setStartDate(s.getStartDate());
        dto.setEndDate(s.getEndDate());
        dto.setStatus(s.getStatus());
        if (p != null) {
            dto.setPaymentId(p.getPaymentId());
            dto.setAmount(p.getAmount());
            dto.setPaymentMethod(p.getPaymentMethod());
            dto.setPaymentStatus(p.getStatus());
        }
        return dto;
    }
}
