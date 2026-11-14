package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.PlanWorkflowDTO.*;
import com.fptu.swp391.sportscentermanager.dto.NotificationResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plan-workflow")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MEMBER', 'COACH')")
public class PlanWorkflowController {
    private final PlanWorkflowService workflow;
    private final NotificationService notifications;

    private Long id(CustomUserDetails user) {
        return user.getUser().getUserId();
    }

    private boolean coach(CustomUserDetails user) {
        return user.getAuthorities().stream()
            .anyMatch(a ->
                "ROLE_COACH".equals(a.getAuthority())
            );
    }

    @GetMapping
    public List<Assignment> list(
        @AuthenticationPrincipal CustomUserDetails user) {
        return workflow.list(id(user), coach(user));
    }

    @GetMapping("/{assignmentId}")
    public Assignment get(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable Long assignmentId) {
        return workflow.get(id(user), coach(user), assignmentId);
    }

    @PostMapping("/{assignmentId}/submit")
    @PreAuthorize("hasRole('MEMBER')")
    public Assignment submit(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable Long assignmentId,
        @Valid @RequestBody Submit input) {
        return workflow.submit(id(user), assignmentId, input);
    }

    @PostMapping("/{assignmentId}/review")
    @PreAuthorize("hasRole('COACH')")
    public Assignment review(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable Long assignmentId,
        @Valid @RequestBody Review input) {
        return workflow.review(id(user), assignmentId, input);
    }

    @GetMapping("/notifications")
    public List<NotificationResponseDTO> notifications(
        @AuthenticationPrincipal CustomUserDetails user) {
        return notifications.getMyNotifications(id(user));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public void read(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable Long notificationId) {
        notifications.markAsRead(id(user), notificationId);
    }
}
