package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.PurchasePreviewResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.entity.Subscription;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionService {
    SubscriptionResponseDTO subscribe(Long memberId, SubscriptionRequestDTO dto);
    List<SubscriptionResponseDTO> getSubscriptions(Long memberId);

    // Gói đang dùng + gói đã thanh toán chờ kích hoạt (member được có nhiều gói cùng lúc, BR-SUB-02)
    List<SubscriptionResponseDTO> getCurrentSubscriptions(Long memberId);

    void markPaid(Subscription subscription, LocalDateTime paidAt);

    void requireMembershipForClass(Long memberId, MembershipPackage pkg, LocalDateTime now);

    // BR-SUB-05: ngày bắt đầu của gói mới, chỉ nối sau gói cùng loại + cùng môn
    LocalDateTime calculateStartDate(Long memberId, MembershipPackage pkg, LocalDateTime now);

    // BR-SUB-08: xem trước ngày dự kiến và cảnh báo gói thừa trước khi thanh toán
    PurchasePreviewResponseDTO previewPurchase(Long memberId, Long packageId);

    // BR-SUB-07: đã có gói ACTIVE cùng loại + cùng tập môn hết hạn sau gói này (đã gia hạn)
    boolean hasRenewal(Subscription subscription);

    void activateMembershipIfWaiting(Long memberId, LocalDateTime now);

    void activateClassPackage(Subscription classPackage, LocalDate classEndDate);

    void releaseClassPackage(Subscription classPackage);

    void extendClassPackages(Long classId, LocalDate newEndDate);

    List<Subscription> autoActivateWaitingMemberships(LocalDateTime now, int waitDays);

    // chặn nếu còn hoá đơn chờ hợp lệ, huỷ đơn VNPay đã hết hạn link
    void cancelStalePendingOrThrow(Long memberId, LocalDateTime now);
}
