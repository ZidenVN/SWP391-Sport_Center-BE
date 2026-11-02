package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public interface CoachService {
    CoachResponseDTO createCoach(CoachRequestDTO requestDTO);
    List<CoachResponseDTO> getAllCoaches();
    CoachResponseDTO getCoachById(Long id);
    List<CoachResponseDTO> getCoachesBySubject(Long subjectId);
    CoachResponseDTO updateCoach(Long id, CoachUpdateRequestDTO requestDTO);
    void toggleCoachStatus(Long id);

    // Lịch dạy theo từng BUỔI (kể cả dạy thay). from/to null thì mặc định hôm qua -> 30 ngày tới
    List<TeachingScheduleResponseDTO> getMyTeachingSchedule(Long coachId, LocalDate from, LocalDate to);
    List<ClassRosterResponseDTO> getClassRoster(Long coachId, Long classId);

    // Danh sách học viên của 1 buổi mình dạy (HLV dạy thay cũng xem được)
    List<ClassRosterResponseDTO> getSessionRoster(Long coachId, Long sessionId);

    // Điểm danh theo BUỔI, chỉ người dạy buổi đó mới được điểm danh
    List<AttendanceResponseDTO> takeAttendance(Long coachId, Long sessionId, TakeAttendanceRequestDTO requestDTO);

    List<AttendanceResponseDTO> getSessionAttendance(Long coachId, Long sessionId);
}
