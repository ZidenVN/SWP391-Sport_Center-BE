package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.ClassRosterResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.CoachRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.CoachResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.TeachingScheduleResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CoachService {
   CoachResponseDTO createCoach(CoachRequestDTO requestDTO);
   List<CoachResponseDTO> getAllCoaches();
   CoachResponseDTO getCoachById(Long id);
   CoachResponseDTO updateCoach(Long id, CoachRequestDTO requestDTO);
   void deleteCoach(Long id);

   List<TeachingScheduleResponseDTO> getMyTeachingSchedule(Long coachId);
   List<ClassRosterResponseDTO> getClassRoster(Long coachId, Long classId);
}
