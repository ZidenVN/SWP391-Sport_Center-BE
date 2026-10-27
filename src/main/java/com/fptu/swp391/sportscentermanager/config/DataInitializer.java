package com.fptu.swp391.sportscentermanager.config;

import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.PermissionCode;
import com.fptu.swp391.sportscentermanager.enums.QuestionType;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.repository.*;
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
    private final QuizQuestionRepository quizQuestionRepository;

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
            PermissionCode.VIEW_OWN_SCHEDULE,
            PermissionCode.SUBSCRIBE_PACKAGE
        )
    );

    @Override
    public void run(String... args) throws Exception {
        Map<String, Permission> permissions = seedPermissions();
        Map<String, Role> roles = seedRoles();
        seedRolePermissions(roles, permissions);
        seedDefaultManager(roles.get("CENTER_MANAGER"));
        seedDefaultCoach(roles.get("COACH"));
        seedDefaultReceptionist(roles.get("RECEPTIONIST"));
        seedQuizSystemQuestions();
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
        if (userRepository.existsByEmail(managerEmail) || userRepository.existsByPhone("0989999999")) return;

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
        if (userRepository.existsByEmail(email) || userRepository.existsByPhone("098789654")) return ;

        if (coachPassword.isBlank()) {
            System.out.println("Chưa cấu hình mật khẩu, bỏ qua tạo tài khoản HLV mặc định");
            return;
        }

        userRepository.save(Coach.builder()
            .firstName("Default")
            .lastName("Coach")
            .gender("MALE")
            .email(email)
            .phone("098789654")
            .passwordHash(passwordEncoder.encode(coachPassword))
            .status(UserStatus.ACTIVE)
            .role(coachRole)
            .speciality("General Fitness")
            .build());
        System.out.println("Đã tạo tài khoản HLV mặc định: " + email);
    }

    private void seedDefaultReceptionist(Role receptionistRole) {
        String email = "receptionist@Sportcenter.com";
        if (userRepository.existsByEmail(email) || userRepository.existsByPhone("0341234567")) return;

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

    private void seedQuizSystemQuestions() {
        seedNumberQuestion("HEIGHT", "Chiều cao của bạn?", 120.0, 230.0, "cm", 1);
        seedNumberQuestion("WEIGHT", "Cân nặng của bạn?", 30.0, 250.0, "kg", 2);

        if (quizQuestionRepository.findByCode("TRAINING_GOAL").isPresent()) return;
        QuizQuestion goal = QuizQuestion.builder()
            .content("Mục tiêu tập luyện chính của bạn?")
            .type(QuestionType.SINGLE)
            .code("TRAINING_GOAL")
            .displayOrder(3)
            .systemQuestion(true)
            .build();
        String[][] options = {
            {"LOSE", "Giảm cân, giảm mỡ"},
            {"GAIN", "Tăng cơ, tăng sức mạnh"},
            {"FLEX", "Dẻo dai, cải thiện tư thế"},
            {"RELAX", "Giảm căng thẳng"},
            {"HEALTH", "Duy trì sức khỏe"}
        };
        for (int i = 0; i < options.length; i++) {
            goal.getOptions().add(QuizOption.builder()
                .question(goal)
                .code(options[i][0])
                .content(options[i][1])
                .displayOrder(i + 1)
                .build());
        }
        quizQuestionRepository.save(goal);
    }

    private void seedNumberQuestion(String code, String content, Double min, Double max, String unit, int order) {
        if (quizQuestionRepository.findByCode(code).isPresent()) return;
        quizQuestionRepository.save(QuizQuestion.builder()
            .content(content)
            .type(QuestionType.NUMBER)
            .code(code)
            .minValue(min)
            .maxValue(max)
            .unit(unit)
            .displayOrder(order)
            .systemQuestion(true)
            .build());
    }

}
