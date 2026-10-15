package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workout_plans")
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class WorkoutPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "plan_name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String planName;

    @Column(name = "exercise_details", nullable = false, columnDefinition = "TEXT")
    private String exerciseDetails;

    @Column(name = "duration", columnDefinition = "NVARCHAR(50)")
    private String duration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id", nullable = false)
    private Coach coach;

    @Column(name = "create_at")
    private LocalDateTime createdAt;
}
