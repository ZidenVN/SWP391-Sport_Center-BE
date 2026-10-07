package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.AuthRequest;
import com.fptu.swp391.sportscentermanager.dto.AuthResponse;
import com.fptu.swp391.sportscentermanager.dto.GoogleLoginRequest;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.security.JwtUtils;
import com.fptu.swp391.sportscentermanager.service.UserService;
import com.google.api.client.auth.openidconnect.IdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.text.html.Option;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private static final String GOOGLE_CLIENT_ID = "621896812018-6c6t3b5b1le9hpkljjd5m9e07mntt3vk.apps.googleusercontent.com";

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            String email = authentication.getName();
            String token = jwtUtils.generateToken(email);
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Sai Email hoặc Password!");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        try {
            String responseMessage = userService.registerUser(user);
            return ResponseEntity.ok(responseMessage);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        try {
            // decode để check Token
            com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier =
                new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(new com.google.api.client.http.javanet.NetHttpTransport(), new com.google.api.client.json.gson.GsonFactory())
                    .setAudience(java.util.Collections.singletonList(GOOGLE_CLIENT_ID)).build();
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
                        .status("ACTIVE")
                        .role(memberRole)
                        .trainingGoal("Chưa cập nhật")
                        .build();
                    userRepository.save(memberUser);
                }

                String jwtToken = jwtUtils.generateToken(email);
                return ResponseEntity.ok(new AuthResponse(jwtToken));
            } else {
                return ResponseEntity.status(401).body("Token Google không hợp lệ");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi xác thực Google: " + e.getMessage());
        }
    }
}
