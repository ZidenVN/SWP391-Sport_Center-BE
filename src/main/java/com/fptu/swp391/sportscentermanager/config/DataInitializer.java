package com.fptu.swp391.sportscentermanager.config;

import com.fptu.swp391.sportscentermanager.entity.Permission;
import com.fptu.swp391.sportscentermanager.entity.Role;
import com.fptu.swp391.sportscentermanager.entity.RolePermission;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.enums.PermissionCode;
import com.fptu.swp391.sportscentermanager.repository.PermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.RolePermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.manager.email:admin@sportcenter.com}")
    private String managerEmail;

    @Value("${app.init.manager.password:toilaadmin}")
    private String managerPassword;

    private static final Map<String, String> ROLES = Map.of(
        "CENTER_MANAGER", "Quản lý trung tâm",
        "RECEPTIONIST", "Lễ tân",
        "COACH", "Huấn luyện viên",
        "MEMBER", "Hội viên"
        );

    private static final Map<String, Set<PermissionCode>> ROLE_PERMISSIONS = Map.of(
        "CENTER_MANAGER", EnumSet.allOf(PermissionCode.class),

        "RECEPTIONIST", EnumSet.of(
            PermissionCode.VIEW_PACKAGE, PermissionCode.VIEW_ROOM,
            PermissionCode.VIEW_SUBJECT, PermissionCode.VIEW_CLASS,
            PermissionCode.CHECK_IN_MEMBER, PermissionCode.VIEW_ATTENDANCE,
            PermissionCode.CREATE_PAYMENT, PermissionCode.VIEW_PAYMENT,
            PermissionCode.VIEW_USER, PermissionCode.CREATE_USER, PermissionCode.UPDATE_USER,
            PermissionCode.VIEW_OWN_SCHEDULE, PermissionCode.UPDATE_OWN_PROFILE,
            PermissionCode.SEARCH_MEMBER,
            PermissionCode.MANAGE_SUBSCRIPTION,
            PermissionCode.MANAGE_TICKET,
            PermissionCode.REGISTER_CLASS
        ),

        "COACH", EnumSet.of(
            PermissionCode.VIEW_CLASS, PermissionCode.VIEW_ROOM, PermissionCode.VIEW_SUBJECT,
            PermissionCode.MARK_ATTENDANCE, PermissionCode.VIEW_ATTENDANCE,
            PermissionCode.VIEW_OWN_PROFILE, PermissionCode.UPDATE_OWN_PROFILE,
            PermissionCode.VIEW_OWN_SCHEDULE
        ),

        "MEMBER", EnumSet.of(
            PermissionCode.VIEW_PACKAGE, PermissionCode.VIEW_CLASS, PermissionCode.REGISTER_CLASS,
            PermissionCode.VIEW_OWN_PROFILE, PermissionCode.UPDATE_OWN_PROFILE,
            PermissionCode.VIEW_OWN_SCHEDULE
        )
    );

    @Override
    public void run(String... args) throws Exception {
        Map<String, Permission> permissions = seedPermissions();
        Map<String, Role> roles = seedRoles();
        seedRolePermissions(roles, permissions);
        seedDefaultManager(roles.get("CENTER_MANAGER"));
    }

    private Map<String, Permission> seedPermissions() {
        Map<String, Permission> result = new HashMap<>();
        for(PermissionCode code : PermissionCode.values()){
            // Hàm findByPermissionName của bạn trả về Object, không phải Optional
            Permission p = permissionRepository.findByPermissionName(code.name());
            if (p == null) {
                System.out.println("Seed permission: " + code.name());
                p = permissionRepository.save(Permission.builder()
                    .permissionName(code.name())
                    .description(code.getDescription())
                    .build());
            }
            result.put(code.name(), p);
        }
        return result;
    }

    private Map<String, Role> seedRoles() {
        Map<String, Role> result = new HashMap<>();
        ROLES.forEach((name, desc) -> {
            Role role = roleRepository.findByRoleName(name);
            if (role == null) {
                System.out.println("Seed role: " + name);
                role = roleRepository.save(Role.builder()
                    .roleName(name)
                    .description(desc)
                    .build());
            }
            result.put(name, role);
        });
        return result;
    }

    private void seedRolePermissions(Map<String, Role> roles, Map<String, Permission> permissions) {
        ROLE_PERMISSIONS.forEach((roleName, codes) -> {
            Role role = roles.get(roleName);
            // Lấy danh sách các quyền hiện tại của Role đó ra trước
            List<RolePermission> existingRPs = rolePermissionRepository.findAllByRole_RoleId(role.getRoleId());

            for (PermissionCode code : codes) {
                Permission permission = permissions.get(code.name());

                // Kiểm tra xem Role này đã có quyền này chưa (tránh lỗi duplicate)
                boolean alreadyHasPermission = existingRPs.stream()
                        .anyMatch(rp -> rp.getPermission().getPermissionId().equals(permission.getPermissionId()));

                if (!alreadyHasPermission) {
                    RolePermission rp = new RolePermission();
                    rp.setRole(role);
                    rp.setPermission(permission);
                    rolePermissionRepository.save(rp);
                }
            }
        });
    }

    private void seedDefaultManager(Role managerRole) {
        if (userRepository.existsByEmail(managerEmail)) return;

        if (managerPassword.isBlank()) {
            System.out.println("Chưa cấu hình mật khẩu, bỏ qua tạo tài khoản Manager mặc định");
            return;
        }

        userRepository.save(User.builder()
            .firstName("System")
            .lastName("Admin")
            .gender("MALE")
            .email(managerEmail)
            .phone("0989999999")
            .passwordHash(passwordEncoder.encode(managerPassword))
            .status("ACTIVE")
            .role(managerRole)
            .build());
        System.out.println("Đã tạo tài khoản Admin mặc định: " + managerEmail);
    }

}
