package com.fptu.swp391.sportscentermanager.entity;

import com.fptu.swp391.sportscentermanager.enums.CheckInType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "check_ins")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CheckIn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_in_id")
    private Long checkInId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptionist_id", nullable = false)
    private Receptionist receptionist;

    @Column(name = "check_in_time" , nullable = false)
    private LocalDateTime checkInTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "type",nullable = false, length = 20)
    @ColumnDefault("'FACILITY'")
    @Builder.Default
    private CheckInType type = CheckInType.FACILITY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_session_id")
    private ClassSession session;
}
