package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.RoleRequest;
import com.fptu.swp391.sportscentermanager.entity.Role;
import com.fptu.swp391.sportscentermanager.service.RoleService;
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

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    @PutMapping("/{id}/permissions")
    public ResponseEntity<Role> updateRolePermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        Role updatedRole = roleService.updateRolePermissions(id, permissionIds);
        return ResponseEntity.ok(updatedRole);
    }
}
