package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fptu.swp391.sportscentermanager.dto.UserResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MyProfileResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MyProfileUpdateDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // Quyền hiện tại của vai trò (CustomUserDetails nạp lại từ DB mỗi request), bỏ ROLE_xxx
    private static List<String> permissionsOf(CustomUserDetails principal) {
        return principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> !a.startsWith("ROLE_"))
            .sorted()
            .toList();
    }

    // Mọi vai trò: FE gọi để cập nhật menu/nút ngay khi Quản lý đổi phân quyền, không cần đăng nhập lại
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/permissions")
    public ResponseEntity<List<String>> myPermissions(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(permissionsOf(principal));
    }

    // Hồ sơ cá nhân cho mọi vai trò, theo quyền VIEW_OWN_PROFILE / UPDATE_OWN_PROFILE trong trang Phân quyền
    @PreAuthorize("hasAuthority('VIEW_OWN_PROFILE')")
    @GetMapping("/me")
    public ResponseEntity<MyProfileResponseDTO> myProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(userService.getMyProfile(principal.getUser().getUserId(), permissionsOf(principal)));
    }

    @LogAudit(actionType = "UPDATE_PROFILE", targetEntity = "User")
    @PreAuthorize("hasAuthority('UPDATE_OWN_PROFILE')")
    @PutMapping("/me")
    public ResponseEntity<MyProfileResponseDTO> updateMyProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                                                @Valid @RequestBody MyProfileUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateMyProfile(principal.getUser().getUserId(), dto, permissionsOf(principal)));
    }

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
    @LogAudit(actionType = "TOGGLE_STATUS", targetEntity = "User")
    @PreAuthorize("hasAuthority('DEACTIVATE_USER') OR hasRole('CENTER_MANAGER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok("Đã cập nhật trạng thái user thành công!");
    }
    @LogAudit(actionType = "ASSIGN_ROLE", targetEntity = "User")
    @PreAuthorize("hasRole('CENTER_MANAGER')")  // chỉ cho phép ROLE CENTER_MANAGER mới gọi được API
    @PatchMapping("/{userId}/role/{roleId}")
    public ResponseEntity<UserResponseDTO> assignRoleToUser(@PathVariable Long userId, @PathVariable Long roleId) {
        return ResponseEntity.ok(userService.assignRoleToUser(userId, roleId));
    }
}
