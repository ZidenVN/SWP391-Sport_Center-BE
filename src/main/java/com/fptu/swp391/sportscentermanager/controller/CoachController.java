package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.ClassRosterResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.CoachRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.CoachResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.TeachingScheduleResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.CoachService;
import com.fptu.swp391.sportscentermanager.dto.MemberProfileResponseDTO;
import com.fptu.swp391.sportscentermanager.service.MemberService;
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

    // API cho MANAGE CENTER

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
    @PreAuthorize("hasAuthority('MANAGE_USER')")
    @PutMapping("/{id}")
    public ResponseEntity<CoachResponseDTO> updateCoach(@PathVariable Long id, @Valid @RequestBody CoachRequestDTO requestDTO) {
        return ResponseEntity.ok(coachService.updateCoach(id, requestDTO));
    }

    @LogAudit(actionType = "DELETE", targetEntity = "Coach")
    @PreAuthorize("hasAuthority('MANAGE_USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCoach(@PathVariable Long id) {
        coachService.deleteCoach(id);
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
}
