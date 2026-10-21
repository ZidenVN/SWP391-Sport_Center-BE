package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.PermissionResponseDTO;

import com.fptu.swp391.sportscentermanager.entity.Role;

import java.util.List;

public interface RoleService {
    List<Role> getAllRoles();
    Role updateRolePermissions(Long roleId, List<Long> permissionIds);
    List<PermissionResponseDTO> getPermissionsOfRole(Long roleId);
}
