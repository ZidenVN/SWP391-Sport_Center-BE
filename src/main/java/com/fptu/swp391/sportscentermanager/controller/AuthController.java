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
                .email(user.getEmail())
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
    public ResponseEntity<?> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        try {
            GoogleProfile profile;
            if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
                profile = profileFromAccessToken(request.getAccessToken());
            } else if (request.getToken() != null && !request.getToken().isBlank()) {
                profile = profileFromIdToken(request.getToken());
            } else {
                return ResponseEntity.status(400).body("Thiếu token Google");
            }
            if (profile == null) {
                return ResponseEntity.status(401).body("Token Google không hợp lệ");
            }
            if (!profile.emailVerified()) {
                return ResponseEntity.status(401).body("Email Google chưa được xác minh");
            }
            String email = profile.email();
            String firstName = profile.givenName();
            String lastName = profile.familyName();

            // kiểm tra db user đã tồn tại chưa; chưa có thì tạo hội viên mới
            java.util.Optional<User> userOption = userRepository.findByEmail(email);
            if (userOption.isEmpty()) {
                com.fptu.swp391.sportscentermanager.entity.Role memberRole = roleRepository.findByRoleName("MEMBER");
                if (memberRole == null) {
                    throw new RuntimeException("Không tìm thấy Role MEMBER");
                }

                Member memberUser = Member.builder()
                    .email(email)
                    .firstName(firstName != null && !firstName.isBlank() ? firstName : email.split("@")[0])
                    .lastName(lastName != null && !lastName.isBlank() ? lastName : "")
                    .gender("OTHER")
                    // phone bắt buộc + unique nhưng Google không cung cấp -> giá trị tạm, FE nhắc hội viên cập nhật
                    .phone("GG_" + System.currentTimeMillis())
                    .passwordHash("LOGIN_BY_GOOGLE")
                    .status(UserStatus.ACTIVE)
                    .role(memberRole)
                    .trainingGoal("Chưa cập nhật")
                    .build();
                userRepository.save(memberUser);
            } else if (userOption.get().getStatus() != UserStatus.ACTIVE) {
                return ResponseEntity.status(403).body("Tài khoản đã bị khóa, vui lòng liên hệ trung tâm");
            }

            User user = userRepository.findByEmail(email).orElse(null);
            String jwtToken = jwtUtils.generateToken(email);
            String roleName = (user != null && user.getRole() != null) ? user.getRole().getRoleName() : "MEMBER";
            String firstNameVal = user != null ? user.getFirstName() : (firstName != null ? firstName : "User");
            String lastNameVal = user != null ? user.getLastName() : (lastName != null ? lastName : "Google");
            String fullNameVal = (firstNameVal + " " + lastNameVal).trim();

            SubscriptionResponseDTO currentPackage = (user != null) ? getActiveSubscription(user.getUserId()) : null;
            String packageName = (currentPackage != null) ? currentPackage.getPackageName() : null;

            return ResponseEntity.ok(AuthResponse.builder()
                .token(jwtToken)
                .email(user != null ? user.getEmail() : email)
                .firstName(firstNameVal)
                .lastName(lastNameVal)
                .fullName(fullNameVal)
                .role(roleName)
                    .permissions(extractPermissions(userDetailsService.loadUserByUsername(email).getAuthorities()))
                .packageName(packageName)
                .currentPackage(currentPackage)
                .build());
        } catch (Exception e) {
            log.warn("Đăng nhập Google thất bại: {}", e.getMessage());
            return ResponseEntity.status(500).body("Lỗi xác thực Google: " + e.getMessage());
        }
    }

    // Thông tin người dùng lấy được từ Google sau khi đã xác minh token
    private record GoogleProfile(String email, boolean emailVerified, String givenName, String familyName) {}

    // ID token (nút chuẩn của Google): tự kiểm chữ ký + audience. Token hỏng định dạng -> null (401),
    // lỗi mạng khi lấy khóa công khai của Google thì ném ra để trả 500
    private GoogleProfile profileFromIdToken(String token) throws Exception {
        com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier =
            new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(new com.google.api.client.http.javanet.NetHttpTransport(), new com.google.api.client.json.gson.GsonFactory())
                .setAudience(java.util.Collections.singletonList(googleClientId)).build();
        GoogleIdToken parsed;
        try {
            parsed = GoogleIdToken.parse(new com.google.api.client.json.gson.GsonFactory(), token);
        } catch (Exception e) {
            return null;
        }
        if (!verifier.verify(parsed)) {
            return null;
        }
        GoogleIdToken.Payload payload = parsed.getPayload();
        // claim chuẩn của Google: given_name = tên, family_name = họ (app lưu firstName = tên, lastName = họ)
        return new GoogleProfile(payload.getEmail(), Boolean.TRUE.equals(payload.getEmailVerified()),
            (String) payload.get("given_name"), (String) payload.get("family_name"));
    }

    // Access token (nút tự thiết kế dùng luồng OAuth): hỏi Google xem token có phải cấp cho CHÍNH Client ID của app không
    // (chặn dùng token lấy từ app khác), rồi mới đọc email / tên từ userinfo
    private GoogleProfile profileFromAccessToken(String accessToken) throws Exception {
        java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpResponse<String> info = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(
                "https://oauth2.googleapis.com/tokeninfo?access_token=" + java.net.URLEncoder.encode(accessToken, java.nio.charset.StandardCharsets.UTF_8)))
            .GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        if (info.statusCode() != 200) {
            return null;
        }
        com.google.gson.JsonObject tokenInfo = com.google.gson.JsonParser.parseString(info.body()).getAsJsonObject();
        String audience = jsonText(tokenInfo, "aud") != null ? jsonText(tokenInfo, "aud") : jsonText(tokenInfo, "azp");
        if (!googleClientId.equals(audience)) {
            return null;
        }
        java.net.http.HttpResponse<String> userInfo = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(
                "https://openidconnect.googleapis.com/v1/userinfo"))
            .header("Authorization", "Bearer " + accessToken).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        if (userInfo.statusCode() != 200) {
            return null;
        }
        com.google.gson.JsonObject u = com.google.gson.JsonParser.parseString(userInfo.body()).getAsJsonObject();
        String email = jsonText(u, "email");
        if (email == null) {
            return null;
        }
        return new GoogleProfile(email, "true".equalsIgnoreCase(jsonText(u, "email_verified")),
            jsonText(u, "given_name"), jsonText(u, "family_name"));
    }

    private static String jsonText(com.google.gson.JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
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
