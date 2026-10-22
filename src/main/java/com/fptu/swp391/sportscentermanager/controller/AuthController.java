package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.AuthRequest;
import com.fptu.swp391.sportscentermanager.dto.AuthResponse;
import com.fptu.swp391.sportscentermanager.dto.GoogleLoginRequest;
import com.fptu.swp391.sportscentermanager.dto.RegisterRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubscriptionResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.security.JwtUtils;
import com.fptu.swp391.sportscentermanager.service.UserService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import com.google.api.client.auth.openidconnect.IdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.text.html.Option;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserDetailsService userDetailsService;


    @Value("${google.client-id}")
    private String googleClientId;

    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            String token = jwtUtils.generateToken(user.getEmail());
            String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "USER";
            String fullName = user.getFirstName() + " " + user.getLastName();

            SubscriptionResponseDTO currentPackage = getActiveSubscription(user.getUserId());
            String packageName = (currentPackage != null) ? currentPackage.getPackageName() : null;

            return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(fullName)
                .role(roleName)
                    .permissions(extractPermissions(userDetails.getAuthorities()))
                .packageName(packageName)
                .currentPackage(currentPackage)
                .build());
        } catch (Exception e) {
            log.warn("Login thất bại {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(401).body("Sai Email hoặc Password!");
        }
    }

    @LogAudit(actionType = "REGISTER_ACCOUNT", targetEntity = "User")
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDTO requestDTO
    ) {
        try {
           User user = new User();
           user.setFirstName(requestDTO.getFirstName());
           user.setLastName(requestDTO.getLastName());
           user.setGender(requestDTO.getGender());
           user.setEmail(requestDTO.getEmail());
           user.setPhone(requestDTO.getPhone());
           user.setPasswordHash(passwordEncoder.encode(requestDTO.getPassword()));
           return  ResponseEntity.ok(userService.registerUser(requestDTO));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            // decode để check Token
            com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier =
                new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(new com.google.api.client.http.javanet.NetHttpTransport(), new com.google.api.client.json.gson.GsonFactory())
                    .setAudience(java.util.Collections.singletonList(googleClientId)).build();
            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = verifier.verify(request.getToken());

            // Token đúng. lấy email và tên từ goole
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String firstName = (String) payload.get("first_name");
                String lastName = (String) payload.get("given_name");

                // kiểm tra db user đã tồn tại chưa
                java.util.Optional<User> userOption = userRepository.findByEmail(email);
                if (userOption.isEmpty()) {
                    com.fptu.swp391.sportscentermanager.entity.Role memberRole = roleRepository.findByRoleName("MEMBER");
                    if (memberRole == null) {
                        throw new RuntimeException("Không tìm thấy Role MEMBER");
                    }

                    Member memberUser = Member.builder()
                        .email(email)
                        .firstName(firstName != null ? firstName : "User")
                        .lastName(lastName != null ? lastName : "Google")
                        .gender("UNKNOWN")
                        .phone("GG_" + System.currentTimeMillis())
                        .passwordHash("LOGIN_BY_GOOGLE")
                        .status(UserStatus.ACTIVE)
                        .role(memberRole)
                        .trainingGoal("Chưa cập nhật")
                        .build();
                    userRepository.save(memberUser);
                }

                User user = userRepository.findByEmail(email).orElse(null);
                String jwtToken = jwtUtils.generateToken(email);
                String roleName = (user != null && user.getRole() != null) ? user.getRole().getRoleName() : "MEMBER";
                String firstNameVal = user != null ? user.getFirstName() : (firstName != null ? firstName : "User");
                String lastNameVal = user != null ? user.getLastName() : (lastName != null ? lastName : "Google");
                String fullNameVal = firstNameVal + " " + lastNameVal;

                SubscriptionResponseDTO currentPackage = (user != null) ? getActiveSubscription(user.getUserId()) : null;
                String packageName = (currentPackage != null) ? currentPackage.getPackageName() : null;

                return ResponseEntity.ok(AuthResponse.builder()
                    .token(jwtToken)
                    .firstName(firstNameVal)
                    .lastName(lastNameVal)
                    .fullName(fullNameVal)
                    .role(roleName)
                        .permissions(extractPermissions(userDetailsService.loadUserByUsername(email).getAuthorities()))
                    .packageName(packageName)
                    .currentPackage(currentPackage)
                    .build());
            } else {
                return ResponseEntity.status(401).body("Token Google không hợp lệ");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi xác thực Google: " + e.getMessage());
        }
    }

    private List<String> extractPermissions(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).filter(a -> !a.startsWith("ROLE_")).toList();
    }

    private SubscriptionResponseDTO getActiveSubscription(Long userId) {
        if (userId == null) return null;
        return subscriptionRepository.findCurrent(userId, LocalDateTime.now())
            .stream()
            .findFirst()
            .map(s -> SubscriptionResponseDTO.builder()
                .subscriptionId(s.getSubscriptionId())
                .packageId(s.getMembershipPackage() != null ? s.getMembershipPackage().getPackageId() : null)
                .packageName(s.getMembershipPackage() != null ? s.getMembershipPackage().getPackageName() : null)
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .status(s.getStatus())
                .build())
            .orElse(null);
    }
}
