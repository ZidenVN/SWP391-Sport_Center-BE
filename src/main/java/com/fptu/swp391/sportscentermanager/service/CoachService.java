package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.*;
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

   List<AttendanceResponseDTO> takeAttendance(Long coachId, Long classId, TakeAttendanceRequestDTO requestDTO);

   List<AttendanceResponseDTO> getClassAttendance(Long coachId, Long classId);
}
