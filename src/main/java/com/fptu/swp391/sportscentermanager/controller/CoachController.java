package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.CoachRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.CoachResponseDTO;
import com.fptu.swp391.sportscentermanager.service.CoachService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coaches")
@RequiredArgsConstructor
public class CoachController {
    private final CoachService coachService;

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

}
