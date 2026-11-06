package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.PermissionResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.RoleResponseDTO;
import com.fptu.swp391.sportscentermanager.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    @GetMapping
    public ResponseEntity<List<RoleResponseDTO>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<PermissionResponseDTO>> getPermissionsOfRole(@PathVariable Long id){
        return ResponseEntity.ok(roleService.getPermissionsOfRole(id));
    }

    @LogAudit(actionType = "UPDATE_PERMISSIONS", targetEntity = "Role")
    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    @PutMapping("/{id}/permissions")
    public ResponseEntity<RoleResponseDTO> updateRolePermissions(@PathVariable Long id, @Valid @RequestBody List<Long> permissionIds) {
        RoleResponseDTO updatedRole = roleService.updateRolePermissions(id, permissionIds);
        return ResponseEntity.ok(updatedRole);
    }
}
