package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PlanWorkflowDTO {
    private PlanWorkflowDTO() {}

    public record Submit(
        @Min(1) int attempt,
        @Size(max = 2000) String note
    ) {}

    public record Review(
        @NotNull Long submissionId,
        @NotBlank
        @Pattern(regexp = "ACCEPT|REDO")
        String decision,
        @Size(max = 2000) String feedback,
        LocalDate newDueDate
    ) {}

    public record Attempt(
        Long submissionId,
        int attempt,
        String memberNote,
        LocalDateTime submittedAt,
        LocalDate dueDateAtSubmission,
        boolean late,
        String decision,
        LocalDateTime reviewedAt,
        String coachFeedback,
        LocalDate extendedDueDate
    ) {}

    public record Assignment(
        Long memberPlanId,
        Long planId,
        String planName,
        String exerciseDetails,
        String memberName,
        Long classId,
        String className,
        String coachName,
        LocalDate assignedDate,
        LocalDate dueDate,
        String status,
        boolean overdue,
        List<Attempt> attempts
    ) {}
}
