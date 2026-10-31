package com.fptu.swp391.sportscentermanager.entity;

import com.fptu.swp391.sportscentermanager.enums.ClassSessionStatus;
import com.fptu.swp391.sportscentermanager.enums.ClassSessionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_sessions", indexes = {
    @Index(name = "idx_session_class_time", columnList = "class_id, start_time"),
    @Index(name = "idx_session_room_time", columnList = "room_id, start_time"),
    @Index(name = "idx_session_coach_time", columnList = "coach_id, start_time")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClassSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SportClass sportClass;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id")
    private Coach coach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @ColumnDefault("'REGULAR'")
    @Builder.Default
    private ClassSessionType type = ClassSessionType.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ColumnDefault("'SCHEDULED'")
    @Builder.Default
    private ClassSessionStatus status = ClassSessionStatus.SCHEDULED;

    // Buổi bù: trỏ tới buổi bị bảo lưu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="makeup_for_id")
    private ClassSession makeupFor;

    @Column(name = "note", length = 255)
    private String note;

}
