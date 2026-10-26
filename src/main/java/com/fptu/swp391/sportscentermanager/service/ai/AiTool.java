package com.fptu.swp391.sportscentermanager.service.ai;

import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;

import java.util.Map;
import java.util.Set;

public interface AiTool {
    String name();
    String description();
    Map<String, Object> parameters();
    Set<String> roles();
    Object execute(Map<String, Object> args, CustomUserDetails user);
}
