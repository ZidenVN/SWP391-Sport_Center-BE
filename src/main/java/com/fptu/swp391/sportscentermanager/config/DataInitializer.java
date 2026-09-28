package com.fptu.swp391.sportscentermanager.config;

import com.fptu.swp391.sportscentermanager.entity.Permission;
import com.fptu.swp391.sportscentermanager.entity.Role;
import com.fptu.swp391.sportscentermanager.repository.PermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        if(permissionRepository.count() == 0){
            List<Permission> permissions = List.of(
                Permission.builder().permissionName("VIEW_USER").description("Xem danh sách người dùng").build(),
                Permission.builder().permissionName("CREATE_USER").description("Tạo người dùng mới").build(),
                Permission.builder().permissionName("UPDATE_USER").description("Cập nhật người dùng").build(),
                Permission.builder().permissionName("DELETE_USER").description("Khóa người dùng").build()
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
        System.out.println("Đã khởi tạo hoàn tất toàn bộ Roles và Permissions mặc định!");
    }
}
