package com.fptu.swp391.sportscentermanager.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/users")
@RestController
public class TestController {
    @GetMapping("/profile")
    public String getProfile(Authentication authentication){
       return "Chào mừng bạn: " + authentication.getName() + " - Chức vụ của bạn là: " + authentication.getAuthorities();
    }
}
