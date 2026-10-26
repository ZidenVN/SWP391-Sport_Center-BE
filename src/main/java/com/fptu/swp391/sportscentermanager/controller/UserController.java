package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.UserUpdateRequestDTO;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.fptu.swp391.sportscentermanager.dto.UserResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PreAuthorize("hasRole('CENTER_MANAGER')")
    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> searchUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) UserStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok(userService.searchUsers(keyword, role, status, page, size));
    }

    @PreAuthorize("hasRole('CENTER_MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @LogAudit(actionType = "UPDATE", targetEntity = "User")
    @PreAuthorize("hasRole('CENTER_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequestDTO requestDTO) {
        return ResponseEntity.ok(userService.updateUser(id, requestDTO));
    }

    @LogAudit(actionType = "TOGGLE_STATUS", targetEntity = "User")
    @PreAuthorize("hasRole('CENTER_MANAGER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> toggleUserStatus(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        userService.toggleUserStatus(id, principal.getUser().getUserId());
        return ResponseEntity.ok("Đã cập nhật trạng thái user thành công!");
    }

    @LogAudit(actionType = "ASSIGN_ROLE", targetEntity = "User")
    @PreAuthorize("hasRole('CENTER_MANAGER')")  // chỉ cho phép ROLE CENTER_MANAGER mới gọi được API
    @PatchMapping("/{userId}/role/{roleId}")
    public ResponseEntity<UserResponseDTO> assignRoleToUser(@PathVariable Long userId, @PathVariable Long roleId) {
        return ResponseEntity.ok(userService.assignRoleToUser(userId, roleId));
    }

    @PreAuthorize("hasAnyRole('CENTER_MANAGER','COACH','RECEPTIONIST')")    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(userService.getUserById(principal.getUser().getUserId()));
    }

    @PreAuthorize("hasAnyRole('CENTER_MANAGER','COACH','RECEPTIONIST')")
    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMe(@AuthenticationPrincipal CustomUserDetails principal,
    @Valid @RequestBody UserUpdateRequestDTO requestDTO) {
        requestDTO.setEmail(null); // khong cho tu doi
        return ResponseEntity.ok(userService.updateUser(principal.getUser().getUserId(), requestDTO));
    }
}
