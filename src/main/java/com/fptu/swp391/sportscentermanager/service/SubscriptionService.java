package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.PurchasePreviewResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;

import java.util.List;
import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import java.time.LocalDateTime;
public interface SubscriptionService {
    SubscriptionResponseDTO subscribe(Long memberId, SubscriptionRequestDTO dto);
    List<SubscriptionResponseDTO> getSubscriptions(Long memberId);

    // Mọi gói đang hiệu lực (member được có nhiều gói cùng lúc, BR-SUB-02)
    List<SubscriptionResponseDTO> getCurrentSubscriptions(Long memberId);

    void markPaid(Subscription subscription, LocalDateTime paidAt);

    void requireMembershipForClass(Long memberId, MembershipPackage pkg, LocalDateTime now);

    // BR-SUB-05: ngày bắt đầu của gói mới, chỉ nối sau gói cùng loại + cùng môn
    LocalDateTime calculateStartDate(Long memberId, MembershipPackage pkg, LocalDateTime now);

    // BR-SUB-08: xem trước ngày dự kiến và cảnh báo gói thừa trước khi thanh toán
    PurchasePreviewResponseDTO previewPurchase(Long memberId, Long packageId);

    // BR-CAN-01, BR-CAN-04: hoàn 1 buổi vào đúng gói đã trừ cho đăng ký này
    @Deprecated
    void refundSession(ClassRegistration registration);

    // BR-SUB-07: đã có gói ACTIVE cùng loại + cùng tập môn hết hạn sau gói này (đã gia hạn)
    boolean hasRenewal(Subscription subscription);
}
