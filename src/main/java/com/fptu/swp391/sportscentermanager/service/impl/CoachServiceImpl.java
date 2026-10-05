package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.CoachRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.CoachResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.entity.Role;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.CoachRepository;
import com.fptu.swp391.sportscentermanager.repository.RoleRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.service.CoachService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

public class CoachServiceImpl implements CoachService {
    private CoachRepository coachRepository;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;

    @Override
    public CoachResponseDTO createCoach(CoachRequestDTO requestDTO) {
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }

        Role coachRole = roleRepository.findByRoleName("COACH");

        Coach newCoach = Coach.builder()
            .firstName(requestDTO.getFirstName())
            .lastName(requestDTO.getLastName())
            .gender(requestDTO.getGender())
            .phone(requestDTO.getPhone())
            .email(requestDTO.getEmail())
            .passwordHash(passwordEncoder.encode(requestDTO.getPassword()))
            .status("ACTIVE")
            .role(coachRole)
            .speciality(requestDTO.getSpecialty())
            .build();

        Coach savedCoach = coachRepository.save(newCoach);
        return CoachResponseDTO.builder()
            .coachId(savedCoach.getUserId())
            .firstName(savedCoach.getFirstName())
            .lastName(savedCoach.getLastName())
            .email(savedCoach.getEmail())
            .phone(savedCoach.getPhone())
            .speciality(savedCoach.getSpeciality())
            .build();
    }

    @Override
    public List<CoachResponseDTO> getAllCoaches() {
        List<Coach> coachList = coachRepository.findAll();
        List<CoachResponseDTO> result = new ArrayList<>();

        for (Coach c : coachList) {
            CoachResponseDTO dto = CoachResponseDTO.builder()
                .coachId(c.getUserId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .speciality(c.getSpeciality())
                .build();
            result.add(dto);
        }
        return result;
    }
}
