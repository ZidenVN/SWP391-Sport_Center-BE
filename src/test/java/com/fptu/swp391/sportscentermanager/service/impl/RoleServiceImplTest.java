package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.entity.Role;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.PermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.RolePermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {
    @Mock RoleRepository roleRepository;
    @Mock PermissionRepository permissionRepository;
    @Mock RolePermissionRepository rolePermissionRepository;
    @InjectMocks RoleServiceImpl roleService;

    @Test
    void updatePermissions_centerManager_isLockedAndNothingDeleted() {
        Role manager = Role.builder().roleId(1L).roleName("CENTER_MANAGER").build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(manager));

        AppException ex = assertThrows(AppException.class, () -> roleService.updateRolePermissions(1L, List.of(1L)));

        assertEquals(ErrorCode.ROLE_LOCKED, ex.getErrorCode());
        verify(rolePermissionRepository, never()).deleteByRole(any());
    }
}
