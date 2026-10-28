package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Role;
import com.fptu.swp391.sportscentermanager.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission,Long> {
    List<RolePermission> findAllByRole_RoleId(Long roleId);

    // BE-TODO 6: lấy tên quyền bằng 1 câu JOIN, tránh N+1 khi load từng Permission LAZY
    @Query("SELECT p.permissionName FROM RolePermission rp JOIN rp.permission p WHERE rp.role.roleId = :roleId")
    List<String> findPermissionNamesByRoleId(@Param("roleId") Long roleId);

    void deleteByRole(Role role);
}
