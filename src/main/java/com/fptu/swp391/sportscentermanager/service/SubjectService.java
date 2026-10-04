package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.SubjectRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubjectResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Subject;

import java.util.List;

public interface SubjectService {
    List<Subject> getAllSubjects();
    Subject getSubjectById(Long id);
    SubjectResponseDTO createSubject(SubjectRequestDTO requestDTO);
    SubjectResponseDTO updateSubject(Long id, SubjectRequestDTO requestDTO);
    void deleteSubjectById(Long id);
}
