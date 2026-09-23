package com.fptu.swp391.sportscentermanager.dto;

public class AuthResponse {
   private String token;
   public AuthResponse (String token) {
       this.token = token;
   }

   public String getToken() {
       return token;
   }
}
