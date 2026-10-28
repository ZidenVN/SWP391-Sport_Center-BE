package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.PurchasePreviewResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.PurchaseWarningDTO;
import com.fptu.swp391.sportscentermanager.dto.SubjectAccessDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.entity.Payment;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.enums.AreaAccess;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.PackageType;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.MembershipPackageRepository;
import com.fptu.swp391.sportscentermanager.repository.PaymentRepository;
import com.fptu.swp391.sportscentermanager.repository.SubjectRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fptu.swp391.sportscentermanager.enums.PaymentMethod;
import com.fptu.swp391.sportscentermanager.enums.PaymentStatus;
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
        LocalDateTime startDate = calculateStartDate(memberId, pkg, now);
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

    @Override
    @Transactional
    public void activate(Subscription subscription, LocalDateTime paidAt) {
        LocalDateTime startDate = calculateStartDate(subscription.getMember().getUserId(),
            subscription.getMembershipPackage(), paidAt);
        subscription.setStartDate(startDate);
        subscription.setEndDate(startDate.plusDays(subscription.getMembershipPackage().getDurationDays()));
        subscription.setStatus("ACTIVE");
        subscriptionRepository.save(subscription);
    }

    // BR-SUB-05: chỉ nối sau gói ACTIVE cùng loại + cùng tập môn, còn hạn và còn buổi.
    // Không có gói như vậy thì bắt đầu ngay.
    @Override
    public LocalDateTime calculateStartDate(Long memberId, MembershipPackage pkg, LocalDateTime now) {
        return subscriptionRepository.findActiveNotEnded(memberId, now).stream()
            .filter(s -> sameScope(s.getMembershipPackage(), pkg))
            .filter(Subscription::hasSessionLeft)
            .map(Subscription::getEndDate)
            .max(Comparator.naturalOrder())
            .orElse(now);
    }

    @Override
    @Transactional
    public PurchasePreviewResponseDTO previewPurchase(Long memberId, Long packageId) {
        if (!memberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);
        }
        MembershipPackage pkg = packageRepository.findByPackageIdAndStatus(packageId, "ACTIVE")
            .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = calculateStartDate(memberId, pkg, now);
        LocalDateTime endDate = startDate.plusDays(pkg.getDurationDays());

        // BR-SUB-08: gói đang có (kể cả chưa bắt đầu) trùng thời gian và vào được cùng khu với gói mới
        Set<Long> newArea = areaSubjectIds(pkg);
        List<PurchaseWarningDTO> warnings = subscriptionRepository.findActiveNotEnded(memberId, now).stream()
            .filter(s -> s.getStartDate().isBefore(endDate) && s.getEndDate().isAfter(startDate))
            .filter(s -> !Collections.disjoint(areaSubjectIds(s.getMembershipPackage()), newArea))
            .map(s -> toWarning(s, pkg, now))
            .toList();

        return PurchasePreviewResponseDTO.builder()
            .packageId(pkg.getPackageId())
            .packageName(pkg.getPackageName())
            .packageType(pkg.getPackageType())
            .startDate(startDate)
            .endDate(endDate)
            .warnings(warnings)
            .build();
    }

    @Override
    @Transactional
    public void refundSession(ClassRegistration registration) {
        // Đăng ký cũ (trước khi tính buổi) không gắn gói thì bỏ qua
        if (registration.getSubscription() == null) return;
        subscriptionRepository.findByIdForUpdate(registration.getSubscription().getSubscriptionId())
            .filter(s -> s.getUsedSessions() > 0)
            .ifPresent(s -> {
                s.setUsedSessions(s.getUsedSessions() - 1);
                subscriptionRepository.save(s);
            });
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

    // Cùng loại + cùng tập môn (FREE toàn trung tâm: cả 2 đều không có môn)
    private boolean sameScope(MembershipPackage a, MembershipPackage b) {
        return a.getPackageType() == b.getPackageType()
            && subjectIds(a).equals(subjectIds(b));
    }

    private Set<Long> subjectIds(MembershipPackage pkg) {
        return pkg.getSubjects().stream().map(Subject::getSubjectId).collect(Collectors.toSet());
    }

    // Các môn mà gói cho vào khu tự tập. FREE toàn trung tâm = mọi môn OPEN (BR-PKG-03)
    private Set<Long> areaSubjectIds(MembershipPackage pkg) {
        if (pkg.getPackageType() == PackageType.FREE && pkg.getSubjects().isEmpty()) {
            return subjectRepository.findByStatusAndAreaAccess("ACTIVE", AreaAccess.OPEN).stream()
                .map(Subject::getSubjectId).collect(Collectors.toSet());
        }
        return subjectIds(pkg);
    }

    private PurchaseWarningDTO toWarning(Subscription s, MembershipPackage newPkg, LocalDateTime now) {
        MembershipPackage owned = s.getMembershipPackage();
        long daysLeft = Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(), s.getEndDate().toLocalDate()));
        String until = s.getEndDate().format(DATE_FMT) + " (còn " + daysLeft + " ngày)";

        String message;
        if (owned.getPackageType() == PackageType.CLASS && newPkg.getPackageType() == PackageType.FREE) {
            message = "Gói " + owned.getPackageName() + " đã bao gồm quyền tự tập đến " + until;
        } else if (owned.getPackageType() == PackageType.FREE && newPkg.getPackageType() == PackageType.CLASS) {
            message = "Bạn đang có gói " + owned.getPackageName() + " đến " + until
                + ". Gói lớp mới đã bao gồm quyền tự tập";
        } else if (owned.getPackageType() == PackageType.FREE && owned.getSubjects().isEmpty()) {
            message = "Gói toàn trung tâm " + owned.getPackageName() + " đã bao gồm khu này đến " + until;
        } else {
            message = "Bạn đang có gói " + owned.getPackageName() + " vào được cùng khu đến " + until;
        }
        Integer remaining = s.remainingSessions();
        if (owned.getPackageType() == PackageType.CLASS && remaining != null) {
            message += ", còn " + remaining + " buổi";
        }

        return PurchaseWarningDTO.builder()
            .subscriptionId(s.getSubscriptionId())
            .packageName(owned.getPackageName())
            .packageType(owned.getPackageType())
            .endDate(s.getEndDate())
            .daysLeft(daysLeft)
            .remainingSessions(remaining)
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
        dto.setWholeCenter(pkg.getPackageType() == PackageType.FREE && pkg.getSubjects().isEmpty());
        dto.setSubjects(pkg.getSubjects().stream()
            .map(sub -> SubjectAccessDTO.builder()
                .subjectId(sub.getSubjectId())
                .subjectName(sub.getSubjectName())
                .areaAccess(sub.getAreaAccess())
                .build())
            .toList());
        dto.setSessionLimit(pkg.getSessionLimit());
        dto.setUsedSessions(s.getUsedSessions());
        dto.setRemainingSessions(s.remainingSessions());
        if (p != null) {
            dto.setPaymentId(p.getPaymentId());
            dto.setAmount(p.getAmount());
            dto.setPaymentMethod(p.getPaymentMethod());
            dto.setPaymentStatus(p.getStatus());
        }
        return dto;
    }
}
