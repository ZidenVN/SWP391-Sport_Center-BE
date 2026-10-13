package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

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
    private String packageName;
    private SubscriptionResponseDTO currentPackage;
}

