package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workout_results")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter @Builder
public class WorkoutResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "activity_detail", columnDefinition = "TEXT")
    private String activityDetails;

    @Column(name = "coach_feedback", columnDefinition = "TEXT")
    private String coachFeedback;
    private LocalDateTime loggedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
