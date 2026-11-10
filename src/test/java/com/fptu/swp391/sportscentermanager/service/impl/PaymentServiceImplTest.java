package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.PaymentResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.*;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.fptu.swp391.sportscentermanager.config.VNPayConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ClassSessionRepository classSessionRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private static final Long PAYMENT_ID = 1L;
    private static final Long SUB_ID = 10L;
    private static final Long CLASS_ID = 100L;

    private Payment paidPayment(long amount, Subscription sub) {
        Payment p = Payment.builder()
            .paymentId(PAYMENT_ID)
            .amount(BigDecimal.valueOf(amount))
            .status(PaymentStatus.COMPLETED.name())
            .subscription(sub)
            .build();
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(p));
        return p;
    }

    private Subscription subscription(PackageType type, SubscriptionStatus status) {
        MembershipPackage pkg = MembershipPackage.builder().packageName("Gói test").packageType(type).build();
        Member member = Member.builder().userId(5L).firstName("An").lastName("Nguyễn").build();
        return Subscription.builder()
            .subscriptionId(SUB_ID)
            .membershipPackage(pkg)
            .member(member)
            .status(status.name())
            .build();
    }

    private void liveRegistration(String classStatus, LocalDateTime firstSessionAt) {
        SportClass sportClass = SportClass.builder().classId(CLASS_ID).status(classStatus).firstSessionAt(firstSessionAt).build();
        ClassRegistration reg = ClassRegistration.builder().sportClass(sportClass).status("REGISTERED").build();
        when(subscriptionRepository.findLiveRegistration(SUB_ID)).thenReturn(Optional.of(reg));
    }

    private void sessions(long total, long postponed, long cancelled) {
        when(classSessionRepository.countBySportClass_ClassId(CLASS_ID)).thenReturn(total);
        when(classSessionRepository.countBySportClass_ClassIdAndStatus(CLASS_ID, SessionStatus.POSTPONED)).thenReturn(postponed);
        when(classSessionRepository.countBySportClass_ClassIdAndStatus(CLASS_ID, SessionStatus.CANCELLED)).thenReturn(cancelled);
    }

    @Test
    @DisplayName("Lớp dừng giữa khoá: hoàn theo tỉ lệ buổi bị huỷ (780.000 x 26/30 = 676.000)")
    void refund_stoppedClass_prorated() {
        Subscription sub = subscription(PackageType.CLASS, SubscriptionStatus.ACTIVE);
        Payment payment = paidPayment(780_000, sub);
        liveRegistration("STOPPED", LocalDateTime.now().minusDays(10));
        sessions(30, 0, 26);

        PaymentResponseDTO res = paymentService.refundPayment(PAYMENT_ID, "Lớp dừng");

        assertEquals(new BigDecimal("676000"), payment.getRefundAmount());
        assertEquals(PaymentStatus.REFUNDED.name(), res.getStatus());
        assertEquals(SubscriptionStatus.REFUNDED.name(), sub.getStatus());
    }

    @Test
    @DisplayName("Lớp dừng: buổi hoãn không tính vào tổng, tiền làm tròn HALF_UP")
    void refund_stoppedClass_excludesPostponedAndRounds() {
        Subscription sub = subscription(PackageType.CLASS, SubscriptionStatus.ACTIVE);
        Payment payment = paidPayment(1_000_000, sub);
        liveRegistration("STOPPED", LocalDateTime.now().minusDays(10));
        // 31 buổi, 1 buổi hoãn (đã có buổi bù) -> tổng 30; 10 buổi bị huỷ -> 333.333,33 -> 333.333
        sessions(31, 1, 10);

        paymentService.refundPayment(PAYMENT_ID, null);

        assertEquals(new BigDecimal("333333"), payment.getRefundAmount());
    }

    @Test
    @DisplayName("Gói lớp đã đăng ký nhưng lớp chưa bắt đầu: hoàn đủ")
    void refund_classNotStarted_fullRefund() {
        Subscription sub = subscription(PackageType.CLASS, SubscriptionStatus.ACTIVE);
        Payment payment = paidPayment(780_000, sub);
        liveRegistration("OPENING", LocalDateTime.now().plusDays(3));

        paymentService.refundPayment(PAYMENT_ID, null);

        assertEquals(new BigDecimal("780000"), payment.getRefundAmount());
        verifyNoInteractions(classSessionRepository);
    }

    @Test
    @DisplayName("Gói đang chờ kích hoạt: hoàn đủ")
    void refund_waitingActivation_fullRefund() {
        Subscription sub = subscription(PackageType.MEMBERSHIP, SubscriptionStatus.WAITING_ACTIVATION);
        Payment payment = paidPayment(1_500_000, sub);
        when(subscriptionRepository.findLiveRegistration(SUB_ID)).thenReturn(Optional.empty());

        paymentService.refundPayment(PAYMENT_ID, null);

        assertEquals(new BigDecimal("1500000"), payment.getRefundAmount());
    }

    @Test
    @DisplayName("Membership đã kích hoạt: không được hoàn")
    void refund_activeMembership_rejected() {
        Subscription sub = subscription(PackageType.MEMBERSHIP, SubscriptionStatus.ACTIVE);
        Payment payment = paidPayment(1_500_000, sub);
        when(subscriptionRepository.findLiveRegistration(SUB_ID)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> paymentService.refundPayment(PAYMENT_ID, null));

        assertEquals(ErrorCode.REFUND_PACKAGE_ALREADY_USED, ex.getErrorCode());
        assertEquals(PaymentStatus.COMPLETED.name(), payment.getStatus());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lớp đang học (chưa dừng): không được hoàn")
    void refund_classInProgress_rejected() {
        Subscription sub = subscription(PackageType.CLASS, SubscriptionStatus.ACTIVE);
        paidPayment(780_000, sub);
        liveRegistration("OPENING", LocalDateTime.now().minusDays(5));

        AppException ex = assertThrows(AppException.class, () -> paymentService.refundPayment(PAYMENT_ID, null));

        assertEquals(ErrorCode.REFUND_PACKAGE_ALREADY_USED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Giao dịch chưa thu tiền: không được hoàn")
    void refund_pendingPayment_rejected() {
        Payment p = Payment.builder().paymentId(PAYMENT_ID).amount(BigDecimal.TEN).status(PaymentStatus.PENDING.name()).build();
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(p));

        AppException ex = assertThrows(AppException.class, () -> paymentService.refundPayment(PAYMENT_ID, null));

        assertEquals(ErrorCode.REFUND_NOT_ALLOWED, ex.getErrorCode());
    }

    // ----- Hội viên tự hủy giao dịch đang chờ -----

    private static final Long OWNER_ID = 5L; // trùng userId trong helper subscription(...)

    private Payment pendingPayment(PaymentMethod method, LocalDateTime createdAt) {
        return Payment.builder()
            .paymentId(PAYMENT_ID)
            .amount(BigDecimal.TEN)
            .paymentMethod(method.name())
            .paymentDate(createdAt)
            .status(PaymentStatus.PENDING.name())
            .subscription(subscription(PackageType.MEMBERSHIP, SubscriptionStatus.PENDING))
            .build();
    }

    private Payment lockedPending(PaymentMethod method) {
        Payment p = pendingPayment(method, LocalDateTime.now());
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    @DisplayName("Hội viên hủy giao dịch VNPay đang chờ của mình")
    void cancelMine_ownVnpayPending_cancelled() {
        Payment p = lockedPending(PaymentMethod.VNPAY);

        PaymentResponseDTO res = paymentService.cancelMyPendingPayment(PAYMENT_ID, OWNER_ID);

        assertEquals(PaymentStatus.CANCELLED.name(), res.getStatus());
        assertEquals(SubscriptionStatus.CANCELLED.name(), p.getSubscription().getStatus());
        assertEquals("Hội viên tự huỷ giao dịch", p.getNote());
        verify(paymentRepository).save(p);
        verify(subscriptionRepository).save(p.getSubscription());
    }

    @Test
    @DisplayName("Hội viên hủy giao dịch tại quầy đang chờ của mình")
    void cancelMine_ownCounterPending_cancelled() {
        Payment p = lockedPending(PaymentMethod.CASH);

        paymentService.cancelMyPendingPayment(PAYMENT_ID, OWNER_ID);

        assertEquals(PaymentStatus.CANCELLED.name(), p.getStatus());
    }

    @Test
    @DisplayName("Không hủy được giao dịch của hội viên khác và không lưu gì")
    void cancelMine_otherMembersPayment_notFound() {
        lockedPending(PaymentMethod.VNPAY);

        AppException ex = assertThrows(AppException.class,
            () -> paymentService.cancelMyPendingPayment(PAYMENT_ID, 999L));

        assertEquals(ErrorCode.PAYMENT_NOT_FOUND, ex.getErrorCode());
        verify(paymentRepository, never()).save(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Giao dịch đã thu tiền thì hội viên không hủy được")
    void cancelMine_completedPayment_rejected() {
        Payment p = lockedPending(PaymentMethod.CASH);
        p.setStatus(PaymentStatus.COMPLETED.name());

        AppException ex = assertThrows(AppException.class,
            () -> paymentService.cancelMyPendingPayment(PAYMENT_ID, OWNER_ID));

        assertEquals(ErrorCode.PAYMENT_ALREADY_PROCESSED, ex.getErrorCode());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Giao dịch không tồn tại")
    void cancelMine_notExists_notFound() {
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
            () -> paymentService.cancelMyPendingPayment(PAYMENT_ID, OWNER_ID));

        assertEquals(ErrorCode.PAYMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("Lễ tân vẫn hủy được giao dịch chờ thu sau khi tách hàm cancel")
    void cancelPending_byReceptionist_keepsReason() {
        Payment p = lockedPending(PaymentMethod.CASH);

        paymentService.cancelPendingPayment(PAYMENT_ID, "Khách đổi ý");

        assertEquals(PaymentStatus.CANCELLED.name(), p.getStatus());
        assertEquals("Khách đổi ý", p.getNote());
    }

    @Test
    @DisplayName("Chỉ VNPay PENDING quá hạn link mới bị đánh dấu Hết hạn")
    void memberPayments_expiredFlag() {
        LocalDateTime old = LocalDateTime.now().minusMinutes(VNPayConfig.PAYMENT_LINK_TTL_MINUTES + 1);
        Payment staleVnpay = pendingPayment(PaymentMethod.VNPAY, old);
        Payment freshVnpay = pendingPayment(PaymentMethod.VNPAY, LocalDateTime.now());
        Payment oldCounter = pendingPayment(PaymentMethod.CASH, old);
        when(memberRepository.existsById(OWNER_ID)).thenReturn(true);
        when(paymentRepository.findByMemberId(OWNER_ID)).thenReturn(List.of(staleVnpay, freshVnpay, oldCounter));

        List<PaymentResponseDTO> res = paymentService.getMemberPayments(OWNER_ID);

        assertTrue(res.get(0).isExpired());
        assertFalse(res.get(1).isExpired());
        assertFalse(res.get(2).isExpired(), "giao dịch tại quầy không có link nên không bao giờ hết hạn");
    }
}
