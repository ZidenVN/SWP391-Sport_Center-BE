package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "members")
@PrimaryKeyJoinColumn(name = "member_id")
@NoArgsConstructor @AllArgsConstructor @Getter @Setter
@SuperBuilder
public class Member extends User {
    @Column(name = "training_goal", nullable = false)
    private String trainingGoal;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "quiz_completed_at")
    private LocalDateTime quizCompletedAt;
}
