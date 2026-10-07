package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.SubscriptionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.entity.Payment;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
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

        // Gia hạn: nếu đang có gói còn hạn thì nối tiếp, không thì bắt đầu từ bây giờ
        LocalDateTime startDate = subscriptionRepository
            .findFirstByMember_UserIdAndStatusOrderByEndDateDesc(memberId, "ACTIVE")
            .map(Subscription::getEndDate)
            .filter(d -> d.isAfter(now))
            .orElse(now);
        LocalDateTime endDate = startDate.plusDays(pkg.getDurationDays());

        Subscription subscription = subscriptionRepository.save(Subscription.builder()
            .member(member)
            .membershipPackage(pkg)
            .startDate(startDate)
            .endDate(endDate)
            .status("ACTIVE")
            .build());

        // Process Payment (include)
        Payment payment = paymentRepository.save(Payment.builder()
            .subscription(subscription)
            .amount(BigDecimal.valueOf(pkg.getPrice()))
            .paymentMethod(dto.getPaymentMethod())
            .paymentDate(now)
            .status("COMPLETED")
            .build());

        return toResponse(subscription, payment);
    }

    @Override
    @Transactional
    public List<SubscriptionResponseDTO> getSubscriptions(Long memberId) {
        return subscriptionRepository.findByMember_UserIdOrderByEndDateDesc(memberId)
            .stream()
            .map(s -> toResponse(s, null))
            .toList();
    }

    @Override
    @Transactional
    public SubscriptionResponseDTO getCurrentSubscription(Long memberId) {
        return subscriptionRepository.findCurrent(memberId, LocalDateTime.now())
            .stream()
            .findFirst()
            .map(s -> toResponse(s, null))
            .orElseThrow(() -> new AppException(ErrorCode.NO_ACTIVE_SUBSCRIPTION));
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
            dto.setAmount(p.getAmount());
            dto.setPaymentMethod(p.getPaymentMethod());
            dto.setPaymentStatus(p.getStatus());
        }
        return dto;
    }
}
