package com.fptu.swp391.sportscentermanager.controller;


import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.MemberProfileResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MemberProfileUpdateDTO;
import com.fptu.swp391.sportscentermanager.dto.WorkoutResultResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.MemberService;
import com.fptu.swp391.sportscentermanager.service.WorkoutResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
    //Controller gọi service
    private final MemberService memberService;
    private final com.fptu.swp391.sportscentermanager.service.WorkoutPlanService workoutPlanService;
    private final WorkoutResultService workoutResultService;

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
    public ResponseEntity<MemberProfileResponseDTO> getMemberProfileById(@PathVariable Long memberId) {
        return ResponseEntity.ok(memberService.getProfile(memberId));
    }

    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/me/workout-result")
    public ResponseEntity<java.util.List<WorkoutResultResponseDTO>> getMyWorkoutResults(@AuthenticationPrincipal CustomUserDetails principal) {
        Long memberId = principal.getUser().getUserId();
        return ResponseEntity.ok(workoutResultService.getResultsByMember(memberId));
    }
}
