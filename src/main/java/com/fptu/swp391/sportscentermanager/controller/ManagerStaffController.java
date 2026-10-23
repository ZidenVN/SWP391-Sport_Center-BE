package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.StaffCreateRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.StaffResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.StaffUpdateRequestDTO;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** BE-25: Center Manager quản lý nhân viên lễ tân. */
@RestController
@RequestMapping("/api/v1/manager/receptionists")
@RequiredArgsConstructor
public class ManagerStaffController {

    private final StaffService staffService;

    // VD: /api/v1/manager/receptionists?keyword=lan&status=ACTIVE
    @PreAuthorize("hasRole('CENTER_MANAGER') and hasAuthority('VIEW_USER')")
    @GetMapping
    public ResponseEntity<List<StaffResponseDTO>> getReceptionists(@RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) UserStatus status) {
        return ResponseEntity.ok(staffService.getReceptionists(keyword, status));
    }

    @PreAuthorize("hasRole('CENTER_MANAGER') and hasAuthority('VIEW_USER')")
    @GetMapping("/{id}")
    public ResponseEntity<StaffResponseDTO> getReceptionist(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.getReceptionist(id));
    }

    @LogAudit(actionType = "CREATE", targetEntity = "Receptionist")
    @PreAuthorize("hasRole('CENTER_MANAGER') and hasAuthority('CREATE_USER')")
    @PostMapping
    public ResponseEntity<StaffResponseDTO> createReceptionist(@Valid @RequestBody StaffCreateRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.createReceptionist(dto));
    }

    @LogAudit(actionType = "UPDATE", targetEntity = "Receptionist")
    @PreAuthorize("hasRole('CENTER_MANAGER') and hasAuthority('UPDATE_USER')")
    @PutMapping("/{id}")
    public ResponseEntity<StaffResponseDTO> updateReceptionist(@PathVariable Long id,
                                                               @Valid @RequestBody StaffUpdateRequestDTO dto) {
        return ResponseEntity.ok(staffService.updateReceptionist(id, dto));
    }

    // Khóa / mở khóa tài khoản lễ tân
    @LogAudit(actionType = "TOGGLE_STATUS", targetEntity = "Receptionist")
    @PreAuthorize("hasRole('CENTER_MANAGER') and hasAuthority('DEACTIVATE_USER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<StaffResponseDTO> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.toggleReceptionistStatus(id));
    }
}
