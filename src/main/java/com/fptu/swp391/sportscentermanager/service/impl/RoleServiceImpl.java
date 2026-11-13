package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.PermissionResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.RoleRequest;
import com.fptu.swp391.sportscentermanager.dto.RoleResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Permission;
import com.fptu.swp391.sportscentermanager.entity.Role;
import com.fptu.swp391.sportscentermanager.entity.RolePermission;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.PermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.RolePermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import com.fptu.swp391.sportscentermanager.service.RoleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository  permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public List<RoleResponseDTO> getAllRoles() {
        return roleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public RoleResponseDTO updateRolePermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        // Quản lý luôn có toàn quyền: gỡ MANAGE_ROLE sẽ tự khoá mình khỏi trang phân quyền
        if ("CENTER_MANAGER".equals(role.getRoleName())) {
            throw new AppException(ErrorCode.ROLE_LOCKED);
        }

        // 1. Xóa toàn bộ quyền cũ của Role này
        rolePermissionRepository.deleteByRole(role);

        // 2. Thêm các quyền mới
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                Permission permission = permissionRepository.findById(permissionId)
                        .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRole(role);
                rolePermission.setPermission(permission);
                rolePermissionRepository.save(rolePermission);
            }
        }
        return toResponse(role);
    }

    @Override
    public List<PermissionResponseDTO> getPermissionsOfRole(Long roleId) {
        if (!roleRepository.existsById(roleId)) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }

        return rolePermissionRepository.findAllByRole_RoleId(roleId).stream().map(rp -> PermissionResponseDTO.builder().permissionId(rp.getPermission().getPermissionId()).permissionName(rp.getPermission().getPermissionName()).description(rp.getPermission().getDescription()).build()).toList();
    }

    private RoleResponseDTO toResponse(Role role) {
        return RoleResponseDTO.builder()
            .roleId(role.getRoleId())
            .roleName(role.getRoleName())
            .description(role.getDescription())
            .build();
    }
}
