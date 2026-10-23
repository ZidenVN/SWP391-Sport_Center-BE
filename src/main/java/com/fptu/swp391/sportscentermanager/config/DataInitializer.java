package com.fptu.swp391.sportscentermanager.config;

import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.PermissionCode;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.repository.PermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.RolePermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbc;

    @Value("${app.init.manager.email:manager@sportcenter.com}")
    private String managerEmail;

    // Không đặt mật khẩu mặc định trong code: để trống thì bỏ qua bước tạo tài khoản
    @Value("${app.init.manager.password:}")
    private String managerPassword;

    @Value("${app.init.coach.password:}")
    private String coachPassword;

    @Value("${app.init.receptionist.password:}")
    private String receptionistPassword;

    private static final Map<String, String> ROLES = new LinkedHashMap<>();
    static {
        ROLES.put("CENTER_MANAGER", "Quản lý trung tâm");
        ROLES.put("COACH", "Huấn luyện viên");
        ROLES.put("RECEPTIONIST", "Lễ tân");
        ROLES.put("MEMBER", "Hội viên");
    }

    private static final Map<String, Set<PermissionCode>> ROLE_PERMISSIONS = Map.of(
        "CENTER_MANAGER", EnumSet.allOf(PermissionCode.class),

        "RECEPTIONIST", EnumSet.of(
            PermissionCode.VIEW_PACKAGE, PermissionCode.VIEW_ROOM,
            PermissionCode.VIEW_SUBJECT, PermissionCode.VIEW_CLASS,
            PermissionCode.CHECK_IN_MEMBER, PermissionCode.VIEW_ATTENDANCE,
            PermissionCode.CREATE_PAYMENT, PermissionCode.VIEW_PAYMENT,
            PermissionCode.VIEW_USER, PermissionCode.CREATE_USER, PermissionCode.UPDATE_USER,
            PermissionCode.VIEW_OWN_SCHEDULE, PermissionCode.VIEW_OWN_PROFILE, PermissionCode.UPDATE_OWN_PROFILE,
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
            PermissionCode.VIEW_OWN_SCHEDULE,
            PermissionCode.SUBSCRIBE_PACKAGE
        )
    );

    @Override
    public void run(String... args) throws Exception {
        Set<String> newPermissions = new HashSet<>();
        Map<String, Permission> permissions = seedPermissions(newPermissions);
        Map<String, Role> roles = seedRoles();
        seedRolePermissions(roles, permissions, newPermissions);
        seedDefaultManager(roles.get("CENTER_MANAGER"));
        seedDefaultCoach(roles.get("COACH"));
        seedDefaultReceptionist(roles.get("RECEPTIONIST"));

        // DB đã có từ trước không nhận quyền mặc định mới nữa (để giữ phân quyền Quản lý đã chỉnh),
        // nên các thay đổi dữ liệu cần thiết được vá đúng 1 lần ở đây.
        applyPatchOnce("2026-10-receptionist-view-own-profile", () -> grantIfMissing(
            roles.get("RECEPTIONIST"), permissions.get(PermissionCode.VIEW_OWN_PROFILE.name())));
        applyPatchOnce("2026-10-default-coach-phone", () -> {
            // SĐT HLV mặc định cũ thiếu 1 số -> HLV không lưu được hồ sơ
            if (!userRepository.existsByPhone("0987896540")) {
                jdbc.update("UPDATE users SET phone = '0987896540' WHERE phone = '098789654'");
            }
        });
    }

    // Đánh dấu trong bảng app_data_patches để mỗi bản vá chỉ chạy 1 lần trên mỗi DB
    private void applyPatchOnce(String key, Runnable patch) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS app_data_patches (patch_key VARCHAR(100) PRIMARY KEY, applied_at DATETIME NOT NULL)");
        Integer done = jdbc.queryForObject("SELECT COUNT(*) FROM app_data_patches WHERE patch_key = ?", Integer.class, key);
        if (done != null && done > 0) return;
        patch.run();
        jdbc.update("INSERT INTO app_data_patches (patch_key, applied_at) VALUES (?, NOW())", key);
        System.out.println("Applied data patch: " + key);
    }

    private void grantIfMissing(Role role, Permission permission) {
        if (role == null || permission == null) return;
        boolean has = rolePermissionRepository.findAllByRole_RoleId(role.getRoleId()).stream()
            .anyMatch(rp -> rp.getPermission().getPermissionId().equals(permission.getPermissionId()));
        if (!has) {
            RolePermission rp = new RolePermission();
            rp.setRole(role);
            rp.setPermission(permission);
            rolePermissionRepository.save(rp);
        }
    }

    private Map<String, Permission> seedPermissions(Set<String> newPermissions) {
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
                newPermissions.add(code.name());
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

    // Không ghi đè phân quyền Quản lý đã chỉnh ở trang Phân quyền: chỉ gán quyền mặc định khi
    // vai trò chưa có quyền nào (DB mới) hoặc khi quyền đó vừa được thêm vào code. Quản lý luôn có toàn quyền.
    private void seedRolePermissions(Map<String, Role> roles, Map<String, Permission> permissions, Set<String> newPermissions) {
        ROLE_PERMISSIONS.forEach((roleName, codes) -> {
            Role role = roles.get(roleName);
            // Lấy danh sách các quyền hiện tại của Role đó ra trước
            List<RolePermission> existingRPs = rolePermissionRepository.findAllByRole_RoleId(role.getRoleId());
            boolean freshRole = existingRPs.isEmpty();
            boolean fullAccess = "CENTER_MANAGER".equals(roleName);

            for (PermissionCode code : codes) {
                if (!fullAccess && !freshRole && !newPermissions.contains(code.name())) continue;
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

        userRepository.save(Manager.builder()
            .firstName("Center")
            .lastName("Manager")
            .gender("MALE")
            .email(managerEmail)
            .phone("0989999999")
            .passwordHash(passwordEncoder.encode(managerPassword))
            .status(UserStatus.ACTIVE)
            .role(managerRole)
            .build());
        System.out.println("Đã tạo tài khoản Center Manager mặc định: " + managerEmail);
    }

    private void seedDefaultCoach(Role coachRole) {
        String email = "coach@Sportcenter.com";
        if (userRepository.existsByEmail(email)) return ;

        if (coachPassword.isBlank()) {
            System.out.println("Chưa cấu hình mật khẩu, bỏ qua tạo tài khoản HLV mặc định");
            return;
        }

        userRepository.save(Coach.builder()
            .firstName("Default")
            .lastName("Coach")
            .gender("MALE")
            .email(email)
            .phone("0987896540")
            .passwordHash(passwordEncoder.encode(coachPassword))
            .status(UserStatus.ACTIVE)
            .role(coachRole)
            .speciality("General Fitness")
            .build());
        System.out.println("Đã tạo tài khoản HLV mặc định: " + email);
    }

    private void seedDefaultReceptionist(Role receptionistRole) {
        String email = "receptionist@Sportcenter.com";
        if (userRepository.existsByEmail(email)) return;

        if (receptionistPassword.isBlank()) {
            System.out.println("Chưa cấu hình mật khẩu, bỏ qua tạo tài khoản Lễ tân mặc định");
            return;
        }

        userRepository.save(Receptionist.builder()
            .firstName("Default")
            .lastName("Receptionist")
            .gender("FEMALE")
            .email(email)
            .phone("0341234567")
            .passwordHash(passwordEncoder.encode(receptionistPassword))
            .status(UserStatus.ACTIVE)
            .role(receptionistRole)
            .build());
        System.out.println("Đã tạo tài khoản Lễ tân mặc định: " + email);
    }

}
