package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.SubscriptionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;

import java.util.List;

public interface SubscriptionService {
    SubscriptionResponseDTO subscribe(Long memberId, SubscriptionRequestDTO dto);
    List<SubscriptionResponseDTO> getSubscriptions(Long memberId);
    SubscriptionResponseDTO getCurrentSubscription(Long memberId);
}
