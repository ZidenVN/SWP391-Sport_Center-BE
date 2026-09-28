package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email is already taken!");
        }
        if (userRepository.existsByPhone(user.getPhone())) {
            throw new RuntimeException("Phone number is already taken!");
        }

        user.setStatus("ACTIVE");
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        userRepository.save(user);
        return "User registered successfully!";
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    @Transactional
    public User updateUser(Long id, User userdetails) {
        User existingUser = getUserById(id);

        existingUser.setFirstName(userdetails.getFirstName());
        existingUser.setLastName(userdetails.getLastName());
        existingUser.setEmail(userdetails.getEmail());
        existingUser.setGender(userdetails.getGender());
        existingUser.setPhone(userdetails.getPhone());

        return existingUser;
    }

    @Override
    public void toggleUserStatus(Long id) {
        User user = getUserById(id);
        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("INACTIVE");
        } else {
            user.setStatus("ACTIVE");
        }
        userRepository.save(user);
    }
}
