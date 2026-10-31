package com.fptu.swp391.sportscentermanager.enums;

// hết hạn = ACTIVE + endDate < now
public enum SubscriptionStatus {
    PENDING,
    WAITING_ACTIVATION,
    ACTIVE,
    CANCELLED,
    REFUNDED
}
