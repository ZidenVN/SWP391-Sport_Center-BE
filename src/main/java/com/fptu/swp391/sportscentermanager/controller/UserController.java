package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fptu.swp391.sportscentermanager.dto.UserResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PreAuthorize("hasAuthority('VIEW_USER') OR hasRole('CENTER_MANAGER')")
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }


    @PreAuthorize("hasAuthority('VIEW_USER') OR hasRole('CENTER_MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @LogAudit(actionType = "UPDATE", targetEntity = "User")
    @PreAuthorize("hasAuthority('UPDATE_USER') OR hasRole('CENTER_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserResponseDTO userDetails) {
        return ResponseEntity.ok(userService.updateUser(id, userDetails));
    }

    @PreAuthorize("hasAuthority('DEACTIVATE_USER') OR hasRole('CENTER_MANAGER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok("Đã cập nhật trạng thái user thành công!");
    }

    @PreAuthorize("hasRole('CENTER_MANAGER')")  // chỉ cho phép ROLE CENTER_MANAGER mới gọi được API
    @PatchMapping("/{userId}/role/{roleId}")
    public ResponseEntity<UserResponseDTO> assignRoleToUser(@PathVariable Long userId, @PathVariable Long roleId) {
        return ResponseEntity.ok(userService.assignRoleToUser(userId, roleId));
    }
}
