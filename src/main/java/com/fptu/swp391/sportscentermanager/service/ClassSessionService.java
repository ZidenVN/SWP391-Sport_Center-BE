package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.ClassSessionResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MakeupSessionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.PostponeSessionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubstituteCoachRequestDTO;

import java.util.List;

public interface ClassSessionService {

    List<ClassSessionResponseDTO> getSessionsByClass(Long classId);

    ClassSessionResponseDTO substituteCoach(Long sessionId, SubstituteCoachRequestDTO dto);

    ClassSessionResponseDTO postponeSession(Long sessionId, PostponeSessionRequestDTO dto);

    ClassSessionResponseDTO createMakeupSession(Long classId, MakeupSessionRequestDTO dto);
}
