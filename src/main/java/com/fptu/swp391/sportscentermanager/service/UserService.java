package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface UserService {
    String registerUser(User user);

    List<User> getAllUsers();
    User getUserById(Long id);
    User updateUser(Long id, User userdetails);
    void toggleUserStatus(Long id);

}
