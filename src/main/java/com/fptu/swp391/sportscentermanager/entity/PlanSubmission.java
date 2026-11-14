package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "plan_submissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_plan_submission_attempt",
        columnNames = {"member_plan_id", "attempt"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long submissionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_plan_id", nullable = false)
    private MemberPlan memberPlan;

    @Column(nullable = false)
    private int attempt;

    @Column(length = 2000)
    private String memberNote;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false)
    private LocalDate dueDateAtSubmission;

    @Column(nullable = false)
    private String decision;

    private LocalDateTime reviewedAt;

    @Column(length = 2000)
    private String coachFeedback;

    private LocalDate extendedDueDate;
}
