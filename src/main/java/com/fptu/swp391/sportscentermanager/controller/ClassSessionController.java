package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.ClassSessionResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MakeupSessionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.PostponeSessionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubstituteCoachRequestDTO;
import com.fptu.swp391.sportscentermanager.service.ClassSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClassSessionController {

    private final ClassSessionService classSessionService;

    @PreAuthorize("hasAuthority('VIEW_CLASS')")
    @GetMapping("/api/v1/classes/{classId}/sessions")
    public ResponseEntity<List<ClassSessionResponseDTO>> getSessionsByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(classSessionService.getSessionsByClass(classId));
    }

    @LogAudit(actionType = "SUBSTITUTE_COACH", targetEntity = "ClassSession")
    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @PatchMapping("/api/v1/sessions/{sessionId}/substitute")
    public ResponseEntity<ClassSessionResponseDTO> substituteCoach(@PathVariable Long sessionId,
                                                                   @Valid @RequestBody SubstituteCoachRequestDTO dto) {
        return ResponseEntity.ok(classSessionService.substituteCoach(sessionId, dto));
    }

    @LogAudit(actionType = "POSTPONE_SESSION", targetEntity = "ClassSession")
    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @PatchMapping("/api/v1/sessions/{sessionId}/postpone")
    public ResponseEntity<ClassSessionResponseDTO> postponeSession(@PathVariable Long sessionId,
                                                                 @Valid @RequestBody PostponeSessionRequestDTO dto) {
        return ResponseEntity.ok(classSessionService.postponeSession(sessionId, dto));
    }

    @LogAudit(actionType = "CREATE_MAKEUP_SESSION", targetEntity = "ClassSession")
    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @PostMapping("/api/v1/classes/{classId}/makeup-sessions")
    public ResponseEntity<ClassSessionResponseDTO> createMakeupSession(@PathVariable Long classId,
                                                                     @Valid @RequestBody MakeupSessionRequestDTO dto) {
        return ResponseEntity.ok(classSessionService.createMakeupSession(classId, dto));
    }
}
