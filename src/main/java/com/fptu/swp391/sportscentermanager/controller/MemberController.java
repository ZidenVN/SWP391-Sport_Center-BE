package com.fptu.swp391.sportscentermanager.controller;


import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.*;
import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.security.CoachAccessGuard;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.MemberService;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.WorkoutResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
    //Controller gọi service
    private final MemberService memberService;
    private final com.fptu.swp391.sportscentermanager.service.WorkoutPlanService workoutPlanService;
    private final WorkoutResultService workoutResultService;
    private final NotificationService notificationService;
    private final CoachAccessGuard coachAccessGuard;

    //TODO: PreAuthorize có tác dụng gì? ND phải cung cấp trong ("") là gì?
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_PROFILE')")
    @GetMapping("/me")
    public ResponseEntity<MemberProfileResponseDTO> getMyProfile(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(memberService.getProfile(principal.getUser().getUserId()));
    }
    @LogAudit(actionType = "UPDATE_PROFILE", targetEntity = "Member")
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('UPDATE_OWN_PROFILE')")
    @PutMapping("/me")
    public ResponseEntity<MemberProfileResponseDTO> updateMyProfile(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody MemberProfileUpdateDTO dto) {
        return ResponseEntity.ok(memberService.updateProfile(principal.getUser().getUserId(), dto));
    }

    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/me/workout-plans")
    public ResponseEntity<java.util.List<com.fptu.swp391.sportscentermanager.dto.WorkoutPlanResponseDTO>> getMyWorkoutPlans(
        @AuthenticationPrincipal CustomUserDetails principal) {
        Long memberId = principal.getUser().getUserId();
        return ResponseEntity.ok(workoutPlanService.getPlansByMember(memberId));
    }

    @PreAuthorize("hasAnyRole('COACH', 'CENTER_MANAGER', 'RECEPTIONIST')")
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberProfileResponseDTO> getMemberProfileById(@PathVariable Long memberId, @AuthenticationPrincipal CustomUserDetails principal) {
        // Manager/Lễ tân xem mọi member; Coach chỉ xem học viên của mình
        if (principal.getUser() instanceof Coach) {
            coachAccessGuard.requireTeachesMember(principal.getUser().getUserId(), memberId);
        }
        return ResponseEntity.ok(memberService.getProfile(memberId));
    }

    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/me/workout-results")
    public ResponseEntity<java.util.List<WorkoutResultResponseDTO>> getMyWorkoutResults(@AuthenticationPrincipal CustomUserDetails principal) {
        Long memberId = principal.getUser().getUserId();
        return ResponseEntity.ok(workoutResultService.getResultsByMember(memberId));
    }

    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/me/progress")
    public ResponseEntity<MemberProgressResponseDTO>  getMyProgress(@AuthenticationPrincipal CustomUserDetails principal) {
        Long memberId = principal.getUser().getUserId();
        return ResponseEntity.ok(workoutResultService.getMemberProgress(memberId));
    }

    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/me/notifications")
    public ResponseEntity<java.util.List<NotificationResponseDTO>> getNotifications(@AuthenticationPrincipal CustomUserDetails principal) {
        Long memberId = principal.getUser().getUserId();
        return ResponseEntity.ok(notificationService.getMyNotifications(memberId));
    }

    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/me/attendances")
    public ResponseEntity<List<MemberAttendanceResponseDTO>> getMyAttendances(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(memberService.getMyAttendances(principal.getUser().getUserId()));
    }
}
