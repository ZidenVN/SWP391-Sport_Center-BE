package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String firstName;
    private String lastName;
    private String fullName;
    private String role;
    private List<String> permissions;
    private String packageName;
    private SubscriptionResponseDTO currentPackage;
}

