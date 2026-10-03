package com.fptu.swp391.sportscentermanager.config;

import com.fptu.swp391.sportscentermanager.entity.Permission;
import com.fptu.swp391.sportscentermanager.entity.Role;
import com.fptu.swp391.sportscentermanager.entity.RolePermission;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.repository.PermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.RolePermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        if(permissionRepository.count() == 0){
            List<Permission> permissions = List.of(
                Permission.builder().permissionName("VIEW_USER").description("Xem danh sách người dùng").build(),
                Permission.builder().permissionName("CREATE_USER").description("Tạo người dùng mới").build(),
                Permission.builder().permissionName("UPDATE_USER").description("Cập nhật người dùng").build(),
                Permission.builder().permissionName("DELETE_USER").description("Khóa người dùng").build(),
                Permission.builder().permissionName("MANAGE_PACKAGE").description("Quản lý gói tập").build(),
                Permission.builder().permissionName("MANAGE_ROLE").description("Quản lý phân quyền").build(),
                Permission.builder().permissionName("MANAGE_ROOM").description("Quản lý phòng tập").build(),
                Permission.builder().permissionName("MANAGER_SUBJECT").description("Quản lý môn học").build()
            );
            permissionRepository.saveAll(permissions);
        }

        if (roleRepository.count() == 0){
            List<Role> roles = List.of(
                Role.builder().roleName("CENTER_MANAGER").description("Quản lý trung tâm").build(),
                Role.builder().roleName("COACH").description("Huấn luyện viên").build(),
                Role.builder().roleName("RECEPTIONIST").description("Lễ tân").build(),
                Role.builder().roleName("MEMBER").description("Hội viên").build()
            );
            roleRepository.saveAll(roles);
        }

        if (userRepository.count() == 0){
            Role adminRole = roleRepository.findById(1L).orElse(null);

            User adminUser = User.builder()
                .firstName("System")
                .lastName("Admin")
                .gender("MALE")
                .email("admin@sportcenter.com")
                .phone("0989999999")
                .passwordHash(passwordEncoder.encode("toilaadmin"))
                .status("ACTIVE")
                .role(adminRole)
                .build();
            userRepository.save(adminUser);
            System.out.println("Đã khởi tạo tài khoản Admin thành công! (admin@sportcenter.com / toilaadmin)");

            // Gán TOÀN BỘ quyền cho Admin Role
            List<Permission> allPermissions = permissionRepository.findAll();
            for (Permission p : allPermissions) {
                RolePermission rp = new RolePermission();
                rp.setRole(adminRole);
                rp.setPermission(p);
                rolePermissionRepository.save(rp);
            }
            System.out.println("Đã gán " + allPermissions.size() + " quyền cho tài khoản Admin!");
        }
    }

}
