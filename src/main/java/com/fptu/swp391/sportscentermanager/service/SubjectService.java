package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.SubjectRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubjectResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Subject;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SubjectService {
    List<Subject> getAllSubjects();
    Subject getSubjectById(Long id);
    SubjectResponseDTO createSubject(SubjectRequestDTO requestDTO, MultipartFile image);
    SubjectResponseDTO updateSubject(Long id, SubjectRequestDTO requestDTO, MultipartFile image);
    void deleteSubjectById(Long id);
}
