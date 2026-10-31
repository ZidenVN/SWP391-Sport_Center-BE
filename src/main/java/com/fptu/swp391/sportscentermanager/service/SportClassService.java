package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.SportClassRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SportClassResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface SportClassService {
    SportClassResponseDTO createClass(SportClassRequestDTO requestDTO, MultipartFile image);
    SportClass getClassById(Long id);
    SportClassResponseDTO getSportClassById(Long id);
    List<SportClassResponseDTO> getAllClasses();
    SportClassResponseDTO assignCoachToClass(Long classId, Long coachId);
    SportClassResponseDTO updateClass(Long id, SportClassRequestDTO requestDTO, MultipartFile image);
    void deleteClass(Long id);

    // BE-15: tìm lớp có filter + phân trang
    Page<SportClassResponseDTO> searchClasses(LocalDate fromDate, LocalDate toDate, Long subjectId, Long coachId,
                                              Long roomId, String status, Boolean available, int page, int size);

    // BE-16: vòng đời lớp
    SportClassResponseDTO cancelClass(Long id, String reason);
    SportClassResponseDTO completeClass(Long id);
    SportClassResponseDTO stopClass(Long id, String reason);
}
