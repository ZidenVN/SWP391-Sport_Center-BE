package com.fptu.swp391.sportscentermanager.dto;

import lombok.Getter;
import lombok.Setter;

// Gửi 1 trong 2:
//  - token: Google ID token (nút "Sign in with Google" chuẩn của Google)
//  - accessToken: access token từ luồng OAuth (nút đăng nhập tự thiết kế trên FE)
@Getter
@Setter
public class GoogleLoginRequest {
    private String token;
    private String accessToken;
}
