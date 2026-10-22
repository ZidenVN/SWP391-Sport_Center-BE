package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.SubscriptionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Subscription;

import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionService {
    SubscriptionResponseDTO subscribe(Long memberId, SubscriptionRequestDTO dto);
    List<SubscriptionResponseDTO> getSubscriptions(Long memberId);
    SubscriptionResponseDTO getCurrentSubscription(Long memberId);

    // BE-17: kích hoạt gói khi giao dịch đã thu tiền (nối tiếp sau gói ACTIVE còn hạn)
    void activate(Subscription subscription, LocalDateTime paidAt);
}
