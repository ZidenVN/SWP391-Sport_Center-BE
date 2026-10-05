package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.CoachRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.CoachResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Coach;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CoachService {
   CoachResponseDTO createCoach(CoachRequestDTO requestDTO);
   List<CoachResponseDTO> getAllCoaches();
}
