package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Lượt hội viên đến trung tâm, lễ tân ghi nhận tại quầy (quyền CHECK_IN_MEMBER). */
@Entity
@Table(name = "member_checkins", indexes = {
    @Index(name = "idx_checkin_member_time", columnList = "member_id, check_in_time"),
    @Index(name = "idx_checkin_time", columnList = "check_in_time")
})
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class MemberCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_in_id")
    private Long checkInId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // Lễ tân / quản lý đã ghi nhận
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_in_by")
    private User checkedInBy;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Column(name = "note", columnDefinition = "nvarchar(255)")
    private String note;
}
