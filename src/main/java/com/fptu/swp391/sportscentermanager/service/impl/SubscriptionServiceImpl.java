package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.PurchasePreviewResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.PurchaseWarningDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.entity.Payment;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.enums.*;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final MembershipPackageRepository packageRepository;
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public SubscriptionResponseDTO subscribe(Long memberId, SubscriptionRequestDTO dto) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        MembershipPackage pkg = packageRepository.findByPackageIdAndStatus(dto.getPackageId(), "ACTIVE")
            .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        requireMembershipForClass(memberId, pkg, now);

        for (Subscription pending : subscriptionRepository.findByMember_UserIdAndStatus(memberId, "PENDING")) {
            Payment pendingPayment = latestPayment(pending);
            if (pendingPayment == null || !PaymentStatus.PENDING.name().equals(pendingPayment.getStatus())) continue;
            boolean staleOnline = PaymentMethod.VNPAY.name().equals(pendingPayment.getPaymentMethod())
                && pendingPayment.getPaymentDate().isBefore(now.minusMinutes(15));
            if (!staleOnline) {
                throw new AppException(ErrorCode.PAYMENT_PENDING_EXISTS);
            }
            // Link VNPay đã hết hạn: hủy để không treo mãi
            pending.setStatus(SubscriptionStatus.CANCELLED.name());
            pendingPayment.setStatus(PaymentStatus.CANCELLED.name());
        }

        Subscription subscription = subscriptionRepository.save(Subscription.builder()
            .member(member)
            .membershipPackage(pkg)
            .status(SubscriptionStatus.PENDING.name())
            .build());


        // Process Payment (include): ghi nhận giao dịch, chưa phải thực thu

        // Process Payment (include)
        Payment payment = paymentRepository.save(Payment.builder()
            .subscription(subscription)
            .amount(BigDecimal.valueOf(pkg.getPrice()))
            .paymentMethod(dto.getPaymentMethod())
            .paymentDate(now)
            .status(PaymentStatus.PENDING.name())
            .build());

        return toResponse(subscription, payment);
    }


    // BR-SUB-05: chỉ nối sau gói ACTIVE cùng loại + cùng tập môn, còn hạn và còn buổi.
    // Không có gói như vậy thì bắt đầu ngay.
    @Override
    public LocalDateTime calculateStartDate(Long memberId, MembershipPackage pkg, LocalDateTime now) {
        return subscriptionRepository.findActiveNotEnded(memberId, now).stream()
            .filter(s -> sameScope(s.getMembershipPackage(), pkg))
            .map(Subscription::getEndDate)
            .max(Comparator.naturalOrder())
            .orElse(now);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchasePreviewResponseDTO previewPurchase(Long memberId, Long packageId) {
        if (!memberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);
        }
        MembershipPackage pkg = packageRepository.findByPackageIdAndStatus(packageId, "ACTIVE")
            .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        requireMembershipForClass(memberId, pkg, now);

        // BR-SUB-08: cảnh báo gói cùng loại (CLASS: cùng môn) đang chờ kích hoạt hoặc còn hạn
        List<PurchaseWarningDTO> warnings = subscriptionRepository.findOwnedNotEnded(memberId, now).stream()
            .filter(s -> sameScope(s.getMembershipPackage(), pkg))
            .map(s -> toWarning(s, now))
            .toList();

        return PurchasePreviewResponseDTO.builder()
            .packageId(pkg.getPackageId())
            .packageName(pkg.getPackageName())
            .packageType(pkg.getPackageType())
            .activationNote(activationNote(pkg))
            .warnings(warnings)
            .build();
    }

    @Override
    public boolean hasRenewal(Subscription subscription) {
        // findActiveNotEnded(member, time) = gói ACTIVE có endDate > time -> truyền endDate của gói đang xét
        return subscriptionRepository.findActiveNotEnded(subscription.getMember().getUserId(), subscription.getEndDate())
            .stream()
            .anyMatch(n -> sameScope(n.getMembershipPackage(), subscription.getMembershipPackage()));
    }

    @Override
    @Transactional
    public void activateMembershipIfWaiting(Long memberId, LocalDateTime now) {
        if (subscriptionRepository.existsCurrentMembership(memberId, now)) {
            return;
        }
        subscriptionRepository.findWaitingMemberships(memberId).stream().findFirst().ifPresent(s -> activateMembership(s, now));
    }

    @Override
    @Transactional
    public void activateClassPackage(Subscription classPackage, LocalDate classEndDate) {
        String status = classPackage.getStatus();
        boolean waiting = SubscriptionStatus.WAITING_ACTIVATION.name().equals(status);
        boolean usable = classPackage.getMembershipPackage().getPackageType() == PackageType.CLASS && (waiting || SubscriptionStatus.ACTIVE.name().equals(status));
        if (!usable) {
            throw new AppException(ErrorCode.CLASS_PACKAGE_NOT_USABLE);
        }

        if (waiting) {
            LocalDateTime now = LocalDateTime.now();
            classPackage.setStatus(SubscriptionStatus.ACTIVE.name());
            classPackage.setStartDate(now);
            classPackage.setActivatedAt(now);
        }

        classPackage.setEndDate(classEndDate.plusDays(1).atStartOfDay());
        subscriptionRepository.save(classPackage);
    }

    @Override
    @Transactional
    public void releaseClassPackage(Subscription classPackage) {
        classPackage.setStatus(SubscriptionStatus.WAITING_ACTIVATION.name());
        classPackage.setStartDate(null);
        classPackage.setEndDate(null);
        classPackage.setActivatedAt(null);
        subscriptionRepository.save(classPackage);
    }

    @Override
    @Transactional
    public void extendClassPackages(Long classId, LocalDate newEndDate) {
        LocalDateTime newEnd = newEndDate.plusDays(1).atStartOfDay();
        for (Subscription s : subscriptionRepository.findClassPackagesOfClass(classId)) {
            if (s.getEndDate() == null || s.getEndDate().isBefore(newEnd)) {
                s.setEndDate(newEnd);
            }
        }
    }

    @Override
    @Transactional
    public List<Subscription> autoActivateWaitingMemberships(LocalDateTime now, int waitDays) {
        List<Subscription> due = subscriptionRepository.findWaitingMembershipsPaidBefore(now.minusDays(waitDays));
        due.forEach(s -> activateMembership(s, now));
        return due;
    }

    private void activateMembership(Subscription s, LocalDateTime now) {
        LocalDateTime start = calculateStartDate(s.getMember().getUserId(), s.getMembershipPackage(), now);
        s.setStartDate(start);
        s.setEndDate(start.plusDays(s.getMembershipPackage().getDurationDays()));
        s.setStatus(SubscriptionStatus.ACTIVE.name());
        s.setActivatedAt(now);
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
    public List<SubscriptionResponseDTO> getCurrentSubscriptions(Long memberId) {
        List<SubscriptionResponseDTO> current = subscriptionRepository.findCurrent(memberId, LocalDateTime.now())
            .stream()
            .map(s -> toResponse(s, latestPayment(s)))
            .toList();
        if (current.isEmpty()) {
            throw new AppException(ErrorCode.NO_ACTIVE_SUBSCRIPTION);
        }
        return current;
    }

    @Override
    public void markPaid(Subscription subscription, LocalDateTime paidAt) {
        subscription.setStatus(SubscriptionStatus.WAITING_ACTIVATION.name());
        subscription.setPaidAt(paidAt);
        subscriptionRepository.save(subscription);
    }

    @Override
    public void requireMembershipForClass(Long memberId, MembershipPackage pkg, LocalDateTime now) {
        if (pkg.getPackageType() == PackageType.CLASS && !subscriptionRepository.existsUsableMembership(memberId, now)) {
            throw new AppException(ErrorCode.MEMBERSHIP_REQUIRED_FOR_CLASS);
        }
    }

    // Cùng loại gói; gói CLASS phải cùng môn
    private boolean sameScope(MembershipPackage a, MembershipPackage b) {
        return a.getPackageType() == b.getPackageType()
            && Objects.equals(subjectIdOf(a), subjectIdOf(b));
    }

    private Long subjectIdOf(MembershipPackage pkg) {
        return pkg.getSubject() != null ? pkg.getSubject().getSubjectId() : null;
    }

    private String activationNote(MembershipPackage pkg) {
        if (pkg.getPackageType() == PackageType.CLASS) {
            return "Gói kích hoạt khi bạn đăng ký lớp, hết hạn theo ngày kết thúc của lớp.";
        }
        return "Gói kích hoạt khi bạn check-in hoặc đăng ký lớp lần đầu, chậm nhất 14 ngày sau khi thanh toán.";
    }

    private PurchaseWarningDTO toWarning(Subscription s, LocalDateTime now) {
        MembershipPackage owned = s.getMembershipPackage();
        boolean waiting = SubscriptionStatus.WAITING_ACTIVATION.name().equals(s.getStatus());

        Long daysLeft = null;
        String message;
        if (waiting) {
            message = "Bạn đang có gói " + owned.getPackageName() + " đã thanh toán nhưng chưa kích hoạt";
        } else {
            daysLeft = Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(), s.getEndDate().toLocalDate()));
            message = "Bạn đang có gói " + owned.getPackageName() + " còn hạn đến "
                + s.getEndDate().format(DATE_FMT) + " (còn " + daysLeft + " ngày)";
        }

        return PurchaseWarningDTO.builder()
            .subscriptionId(s.getSubscriptionId())
            .packageName(owned.getPackageName())
            .packageType(owned.getPackageType())
            .status(s.getStatus())
            .endDate(s.getEndDate())
            .daysLeft(daysLeft)
            .message(message)
            .build();
    }

    // Lấy giao dịch gần nhất của gói để hiển thị amount / paymentMethod / paymentStatus
    private Payment latestPayment(Subscription s) {
        return paymentRepository
            .findFirstBySubscription_SubscriptionIdOrderByPaymentDateDesc(s.getSubscriptionId())
            .orElse(null);
    }

    private SubscriptionResponseDTO toResponse(Subscription s, Payment p) {
        MembershipPackage pkg = s.getMembershipPackage();
        SubscriptionResponseDTO dto = new SubscriptionResponseDTO();
        dto.setSubscriptionId(s.getSubscriptionId());
        dto.setPackageId(pkg.getPackageId());
        dto.setPackageName(pkg.getPackageName());
        dto.setStartDate(s.getStartDate());
        dto.setEndDate(s.getEndDate());
        dto.setStatus(s.getStatus());
        dto.setPackageType(pkg.getPackageType());
        dto.setSubjectId(subjectIdOf(pkg));
        dto.setSubjectName(pkg.getSubject() != null ? pkg.getSubject().getSubjectName() : null);
        dto.setPaidAt(s.getPaidAt());
        dto.setActivatedAt(s.getActivatedAt());
        if (p != null) {
            dto.setPaymentId(p.getPaymentId());
            dto.setAmount(p.getAmount());
            dto.setPaymentMethod(p.getPaymentMethod());
            dto.setPaymentStatus(p.getStatus());
        }
        return dto;
    }
}
