package com.fptu.swp391.sportscentermanager.entity;

import com.fptu.swp391.sportscentermanager.enums.SessionStatus;
import com.fptu.swp391.sportscentermanager.enums.SessionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_sessions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClassSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SportClass sportClass;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id", nullable = false)
    private Coach coach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SessionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "makeup_for_id")
    private ClassSession makeupFor;

    @Column(name = "note", length = 500)
    private String note;
}
