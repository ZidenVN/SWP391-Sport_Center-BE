package com.fptu.swp391.sportscentermanager.service.impl;
import com.fptu.swp391.sportscentermanager.dto.PlanWorkflowDTO.*;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.fptu.swp391.sportscentermanager.service.PlanWorkflowService;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import java.time.*;
import java.util.*;
@Service
@RequiredArgsConstructor
@Transactional
public class PlanWorkflowServiceImpl implements PlanWorkflowService{
    public static final ZoneId ZONE =
        ZoneId.of("Asia/Ho_Chi_Minh");

    private final MemberPlanRepository assignments;
    private final PlanSubmissionRepository submissions;
    private final NotificationService notifications;

    @Transactional(readOnly = true)
    public List<Assignment> list(Long userId, boolean coach) {
        var rows = coach
            ? assignments.findByWorkoutPlan_Coach_UserId(userId)
            : assignments.findByMember_UserId(userId);

        return rows.stream()
            .sorted(
                Comparator.comparing(MemberPlan::getMemberPlanId)
                    .reversed()
            )
            .map(this::view)
            .toList();
    }

    @Transactional(readOnly = true)
    public Assignment get(Long userId, boolean coach, Long id) {
        var row = assignments.findById(id)
            .orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "Không có giáo án này")
            );

        requireOwner(row, userId, coach);
        return view(row);
    }

    public Assignment submit(Long memberId, Long id, Submit input) {
        var row = lock(id);
        requireOwner(row, memberId, false);

        var history =
            submissions.findByMemberPlan_MemberPlanIdOrderByAttemptAsc(id);

        String note = clean(input.note());

        var repeated = history.stream()
            .filter(s -> s.getAttempt() == input.attempt())
            .findFirst();

        if (repeated.isPresent()) {
            if (!Objects.equals(
                repeated.get().getMemberNote(), note)) {
                throw error(
                    HttpStatus.CONFLICT,
                    "Lần báo này đã tồn tại với nội dung khác. Hãy tải lại."
                );
            }

            // Retry cùng lần báo: không tạo thêm lịch sử/thông báo.
            return view(row);
        }

        if (input.attempt() != history.size() + 1) {
            throw error(
                HttpStatus.CONFLICT,
                "Dữ liệu đã thay đổi. Hãy tải lại."
            );
        }

        if (!actionable(row.getStatus())) {
            throw error(
                HttpStatus.CONFLICT,
                "Giáo án đang chờ duyệt hoặc đã hoàn thành"
            );
        }

        if (row.getDueDate() == null) {
            throw error(
                HttpStatus.CONFLICT,
                "Giáo án cũ chưa có hạn. Cần Coach giao lại theo luồng mới."
            );
        }

        var now = LocalDateTime.now(ZONE);

        var submission = PlanSubmission.builder()
            .memberPlan(row)
            .attempt(input.attempt())
            .memberNote(note)
            .submittedAt(now)
            .dueDateAtSubmission(row.getDueDate().toLocalDate())
            .decision("PENDING_REVIEW")
            .build();

        submissions.save(submission);
        row.setStatus("PENDING_REVIEW");

        var coach = row.getWorkoutPlan().getCoach();

        notifications.notifyUsers(
            List.of(coach),
            "Học viên báo hoàn thành giáo án",
            name(row.getMember())
                + " · " + origin(row)
                + " · " + row.getWorkoutPlan().getPlanName()
                + " · Báo lúc " + now + " (giờ Việt Nam)"
                + " · "
                + (
                now.toLocalDate()
                    .isAfter(submission.getDueDateAtSubmission())
                    ? "Nộp trễ"
                    : "Đúng hạn"
            ),
            "PLAN_REVIEW",
            null,
            "/coach/plan-reviews/" + id
        );

        return view(row);
    }

    public Assignment review(Long coachId, Long id, Review input) {
        var row = lock(id);
        requireOwner(row, coachId, true);

        var history =
            submissions.findByMemberPlan_MemberPlanIdOrderByAttemptAsc(id);

        if (history.isEmpty()) {
            throw error(
                HttpStatus.CONFLICT,
                "Học viên chưa báo hoàn thành"
            );
        }

        var latest = history.get(history.size() - 1);

        if (!latest.getSubmissionId().equals(input.submissionId())) {
            throw error(
                HttpStatus.CONFLICT,
                "Bạn đang duyệt lần báo cũ. Hãy tải lại."
            );
        }

        String feedback = clean(input.feedback());

        if (!"PENDING_REVIEW".equals(latest.getDecision())) {
            boolean sameReview =
                latest.getDecision().equals(input.decision())
                    && Objects.equals(
                    latest.getCoachFeedback(), feedback
                )
                    && Objects.equals(
                    latest.getExtendedDueDate(), input.newDueDate()
                );

            if (sameReview) {
                return view(row);
            }

            throw error(
                HttpStatus.CONFLICT,
                "Lần báo này đã được duyệt"
            );
        }

        if (!"PENDING_REVIEW".equals(row.getStatus())) {
            throw error(
                HttpStatus.CONFLICT,
                "Giáo án không ở trạng thái chờ duyệt"
            );
        }

        boolean accept = "ACCEPT".equals(input.decision());

        if (!accept && feedback == null) {
            throw error(
                HttpStatus.BAD_REQUEST,
                "Cần ghi lý do yêu cầu làm lại"
            );
        }

        if (accept && input.newDueDate() != null) {
            throw error(
                HttpStatus.BAD_REQUEST,
                "Chỉ gia hạn khi yêu cầu làm lại"
            );
        }

        if (input.newDueDate() != null) {
            var due = input.newDueDate();

            if (due.isBefore(LocalDate.now(ZONE))
                || due.isBefore(row.getDueDate().toLocalDate())) {
                throw error(
                    HttpStatus.BAD_REQUEST,
                    "Hạn mới phải từ hôm nay và không sớm hơn hạn hiện tại"
                );
            }

            row.setDueDate(due.atTime(23, 59, 59));
        }

        latest.setDecision(input.decision());
        latest.setCoachFeedback(feedback);
        latest.setReviewedAt(LocalDateTime.now(ZONE));
        latest.setExtendedDueDate(input.newDueDate());

        row.setStatus(accept ? "COMPLETED" : "NEEDS_REVISION");

        notifications.notifyUsers(
            List.of(row.getMember()),
            accept
                ? "Coach đã xác nhận hoàn thành"
                : "Coach yêu cầu làm lại",
            row.getWorkoutPlan().getPlanName()
                + " · " + origin(row)
                + (feedback == null ? "" : " · " + feedback),
            "PLAN_RESULT",
            null,
            "/member/plan-workflow/" + id
        );

        return view(row);
    }

    private MemberPlan lock(Long id) {
        return assignments.findLocked(id)
            .orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "Không có giáo án này")
            );
    }

    private void requireOwner(
        MemberPlan row, Long userId, boolean coach) {

        Long owner = coach
            ? row.getWorkoutPlan().getCoach().getUserId()
            : row.getMember().getUserId();

        if (!owner.equals(userId)) {
            throw error(
                HttpStatus.FORBIDDEN,
                "Bạn không có quyền với giáo án này"
            );
        }
    }

    private Assignment view(MemberPlan row) {
        var history =
            submissions.findByMemberPlan_MemberPlanIdOrderByAttemptAsc(
                row.getMemberPlanId()
            );

        var due = row.getDueDate() == null
            ? null
            : row.getDueDate().toLocalDate();

        boolean overdue =
            due != null
                && LocalDate.now(ZONE).isAfter(due)
                && actionable(row.getStatus());

        return new Assignment(
            row.getMemberPlanId(),
            row.getWorkoutPlan().getPlanId(),
            row.getWorkoutPlan().getPlanName(),
            row.getWorkoutPlan().getExerciseDetails(),
            name(row.getMember()),
            row.getSourceClassId(),
            origin(row),
            name(row.getWorkoutPlan().getCoach()),
            row.getAssignedDate(),
            due,
            row.getStatus(),
            overdue,
            history.stream()
                .map(s -> new Attempt(
                    s.getSubmissionId(),
                    s.getAttempt(),
                    s.getMemberNote(),
                    s.getSubmittedAt(),
                    s.getDueDateAtSubmission(),
                    s.getSubmittedAt().toLocalDate()
                        .isAfter(s.getDueDateAtSubmission()),
                    s.getDecision(),
                    s.getReviewedAt(),
                    s.getCoachFeedback(),
                    s.getExtendedDueDate()
                ))
                .toList()
        );
    }

    private String origin(MemberPlan row) {
        return row.getSourceClassId() == null
            ? "Giáo án cá nhân"
            : "Lớp #" + row.getSourceClassId()
            + " · " + row.getSourceClassName();
    }

    private boolean actionable(String status) {
        return "IN_PROGRESS".equals(status)
            || "NEEDS_REVISION".equals(status);
    }

    private String name(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private String clean(String value) {
        return value == null || value.isBlank()
            ? null
            : value.trim();
    }

    private ResponseStatusException error(
        HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
