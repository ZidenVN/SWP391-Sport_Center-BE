package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Một member đăng ký một lớp học.
 * - status: REGISTERED (đang đăng ký) | CANCELLED (đã hủy)
 * - Mỗi cặp (member, class) chỉ có 1 dòng. Hủy rồi đăng ký lại thì dùng lại dòng cũ.
 */
@Entity
@Table(name = "class_registrations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "class_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    private Long registrationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SportClass sportClass;

    @Column(nullable = false)
    private String status;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;
}
