package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.*;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.CoachService;
import com.fptu.swp391.sportscentermanager.service.MemberService;
import com.fptu.swp391.sportscentermanager.service.WorkoutPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coaches")
@RequiredArgsConstructor
public class CoachController {
    private final CoachService coachService;
    private final MemberService memberService;
    private final WorkoutPlanService workoutPlanService;

    // API cho MANAGE CENTER

    @PreAuthorize("hasAuthority('VIEW_USER')")
    @GetMapping("/{id}")
    public ResponseEntity<CoachResponseDTO> getCoachById(@PathVariable Long id) {
        return ResponseEntity.ok(coachService.getCoachById(id));
    }

    @LogAudit(actionType = "CREATE", targetEntity = "Coach")
    @PreAuthorize("hasAuthority('CREATE_USER')")
    @PostMapping
    public ResponseEntity<CoachResponseDTO> createCoach(@Valid @RequestBody CoachRequestDTO requestDTO) {
        return ResponseEntity.ok(coachService.createCoach(requestDTO));
    }

    @PreAuthorize("hasAuthority('VIEW_USER')")
    @GetMapping
    public ResponseEntity<List<CoachResponseDTO>> getAllCoaches() {
        return ResponseEntity.ok(coachService.getAllCoaches());
    }

    @LogAudit(actionType = "UPDATE", targetEntity = "Coach")
    @PreAuthorize("hasAuthority('UPDATE_USER') OR hasRole('CENTER_MANAGER') ")
    @PutMapping("/{id}")
    public ResponseEntity<CoachResponseDTO> updateCoach(@PathVariable Long id, @Valid @RequestBody CoachRequestDTO requestDTO) {
        return ResponseEntity.ok(coachService.updateCoach(id, requestDTO));
    }

    @LogAudit(actionType = "DELETE", targetEntity = "Coach")
    @PreAuthorize("hasAuthority('DEACTIVATE_USER') OR hasRole('CENTER_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCoach(@PathVariable Long id) {
        coachService.toggleCoachStatus(id);
        return ResponseEntity.ok("Đã khóa tài khoản thành công!");
    }


    // API cho COACH
    @PreAuthorize("hasRole('COACH') and hasAuthority('VIEW_OWN_SCHEDULE')")
    @GetMapping("/me/schedules")
    public ResponseEntity<List<TeachingScheduleResponseDTO>> getMySchedules(@AuthenticationPrincipal CustomUserDetails principal) {
        Long coachId = principal.getUser().getUserId();
        return ResponseEntity.ok(coachService.getMyTeachingSchedule(coachId));
    }

    @PreAuthorize("hasRole('COACH') and hasAuthority('VIEW_CLASS')")
    @GetMapping("/me/classes/{classId}/roster")
    public ResponseEntity<List<ClassRosterResponseDTO>> getClassRoster(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long classId) {
        Long coachId = principal.getUser().getUserId();
        return ResponseEntity.ok(coachService.getClassRoster(coachId, classId));
    }

    /**
     * Coach xem chi tiết hồ sơ và mục tiêu tập luyện (goals) của học viên
     */
    @PreAuthorize("hasRole('COACH')")
    @GetMapping("/members/{memberId}")
    public ResponseEntity<MemberProfileResponseDTO> getMemberProfileAndGoal(@PathVariable Long memberId) {
        return ResponseEntity.ok(memberService.getProfile(memberId));
    }

    @LogAudit(actionType = "MARK_ATTENDANCE", targetEntity = "ClassAttendance")
    @PreAuthorize("hasRole('COACH') AND hasAuthority('MARK_ATTENDANCE')")
    @PostMapping("/me/classes/{classId}/attendances")
    public ResponseEntity<List<AttendanceResponseDTO>> takeAttendance(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long classId, @Valid @RequestBody TakeAttendanceRequestDTO requestDTO) {
Long coachId = principal.getUser().getUserId();
return ResponseEntity.ok(coachService.takeAttendance(coachId, classId, requestDTO));
    }

    @PreAuthorize("hasRole('COACH') AND hasAuthority('VIEW_ATTENDANCE')")
    @GetMapping("/me/classes/{classId}/attendances")
    public ResponseEntity<List<AttendanceResponseDTO>> getClassAttendances(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long classId) {
        Long coachId = principal.getUser().getUserId();
        return ResponseEntity.ok(coachService.getClassAttendance(coachId, classId));
    }

    // coach giao bài tập luyện cho cá nhân hoặc cho lớp
    @LogAudit(actionType = "ASSIGN_WORKOUT_PLAN", targetEntity = "WorkoutPlan")
    @PreAuthorize("hasRole('COACH')")
    @PostMapping("/me/workout-plans")
    public ResponseEntity<WorkoutPlanResponseDTO> createAndAssignWorkoutPlan(@AuthenticationPrincipal CustomUserDetails principal, @Valid @RequestBody WorkoutPlanRequestDTO requestDTO) {
        Long coachId = principal.getUser().getUserId();
        return ResponseEntity.ok(workoutPlanService.createAndAssignWorkoutPlan(coachId, requestDTO));
    }

    // Xem tat ca giao an ma Coach tao ra
    @PreAuthorize("hasRole('COACH')")
    @GetMapping("/me/workout-plans")
    public ResponseEntity<List<WorkoutPlanResponseDTO>> getMyWorkoutPlans(@AuthenticationPrincipal CustomUserDetails principal) {
        Long coachId = principal.getUser().getUserId();
        return ResponseEntity.ok(workoutPlanService.getPlansByCoach(coachId));
    }

    // Xem giao an cua mot hoc vien cu the
    @PreAuthorize("hasRole('COACH')")
    @GetMapping("/members/{memberId}/workout-plans")
    public ResponseEntity<List<WorkoutPlanResponseDTO>> getMemberWorkoutPlans(@PathVariable Long memberId) {
        return ResponseEntity.ok(workoutPlanService.getPlansByMember(memberId));
    }

    // xem giao an hien tai da gan cho mot lop cu the nao
    @PreAuthorize("hasRole('COACH') OR hasRole('MEMBER')")
    @GetMapping("/classes/{classId}/workout-plans")
    public ResponseEntity<List<WorkoutPlanResponseDTO>> getClassWorkoutPlans(@PathVariable Long classId) {
        return ResponseEntity.ok(workoutPlanService.getPlansByClass(classId));
    }

}
