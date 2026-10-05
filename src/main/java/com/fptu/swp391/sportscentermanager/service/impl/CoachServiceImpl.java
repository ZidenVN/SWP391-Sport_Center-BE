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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoachServiceImpl implements CoachService {
    private final CoachRepository coachRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Override
    public CoachResponseDTO getCoachById(Long id) {
        Coach c = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        if ("INACTIVE".equals(c.getStatus())) {
            throw new AppException(ErrorCode.COACH_NOT_FOUND);
        }
        return CoachResponseDTO.builder()
            .coachId(c.getUserId())
            .firstName(c.getFirstName())
            .lastName(c.getLastName())
            .phone(c.getPhone())
            .email(c.getEmail())
            .speciality(c.getSpeciality())
            .build();
    }

    @Override
    public CoachResponseDTO updateCoach(Long id, CoachRequestDTO requestDTO) {
        Coach coach = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        if (!coach.getEmail().equals(requestDTO.getEmail()) && userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }

        coach.setFirstName(requestDTO.getFirstName());
        coach.setLastName(requestDTO.getLastName());
        coach.setGender(requestDTO.getGender());
        coach.setPhone(requestDTO.getPhone());
        coach.setEmail(requestDTO.getEmail());
        coach.setSpeciality(requestDTO.getSpecialty());

        if (requestDTO.getPassword() != null && !requestDTO.getPassword().isBlank()){
            coach.setPasswordHash(passwordEncoder.encode(requestDTO.getPassword()));
        }

        Coach savedCoach = coachRepository.save(coach);

        return CoachResponseDTO.builder()
            .coachId(savedCoach.getUserId())
            .firstName(savedCoach.getFirstName())
            .lastName(savedCoach.getLastName())
            .phone(savedCoach.getPhone())
            .speciality(savedCoach.getSpeciality())
            .build();
    }

    @Override
    public void deleteCoach(Long id) {
        Coach coach = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        coach.setStatus("INACTIVE");
        coachRepository.save(coach);
    }

}

