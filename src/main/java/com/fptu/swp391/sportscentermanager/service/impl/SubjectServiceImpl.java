package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.SubjectRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubjectResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.repository.SubjectRepository;
import com.fptu.swp391.sportscentermanager.service.SubjectService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.fptu.swp391.sportscentermanager.dto.SubjectResponseDTO.*;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;

    @Override
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @Override
    public Subject getSubjectById(Long id) {
        return subjectRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy Bộ môn với ID " + id));
    }

    @Override
    @Transactional
    public SubjectResponseDTO createSubject(SubjectRequestDTO subjectRequestDTO) {
            Subject newSubject = Subject.builder()
                .subjectName(subjectRequestDTO.getSubjectName())
                .description(subjectRequestDTO.getDescription())
                .build();
            Subject savedSubject = subjectRepository.save(newSubject);

            return SubjectResponseDTO.builder()
                .subjectId(savedSubject.getSubjectId())
                .subjectName(savedSubject.getSubjectName())
                .description(savedSubject.getDescription())
                .build();

    }

    @Override
    public SubjectResponseDTO updateSubject(Long id, SubjectRequestDTO requestDTO) {
       Subject existingSubject = getSubjectById(id);
       existingSubject.setSubjectName(requestDTO.getSubjectName());
       existingSubject.setDescription(requestDTO.getDescription());

       Subject updatedSubject = subjectRepository.save(existingSubject);
       return SubjectResponseDTO.builder()
           .subjectName(updatedSubject.getSubjectName())
           .description(updatedSubject.getDescription())
           .build();
    }

    @Override
    public void deleteSubjectById(Long id) {
        Subject subject = getSubjectById(id);
        subjectRepository.deleteById(id);
    }
}
