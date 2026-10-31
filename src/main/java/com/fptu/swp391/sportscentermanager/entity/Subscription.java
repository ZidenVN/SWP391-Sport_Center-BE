package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;
@Entity
@Table(name = "subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "start_date")
    private LocalDateTime startDate;
    @Column(name = "end_date")
    private LocalDateTime endDate;
    private String status;
    private String attribute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private MembershipPackage membershipPackage;

    @Column(name = "expiry_reminder_sent_at")
    private LocalDateTime expiryReminderSentAt;

    // Số buổi đã dùng (trừ lúc đăng ký lớp, hoàn lúc huỷ)
    @Column(name = "used_sessions", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    @Deprecated
    private Integer usedSessions = 0;

    // Số buổi còn lại, null = không giới hạn (gói CLASS không giới hạn hoặc gói FREE)
    public Integer remainingSessions() {
        Integer limit = membershipPackage.getSessionLimit();
        return limit == null ? null : Math.max(0, limit - usedSessions);
    }

    public boolean hasSessionLeft() {
        Integer remaining = remainingSessions();
        return remaining == null || remaining > 0;
    }

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "receptionist_switch_count", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private Integer receptionistSwitchCount = 0;

}
