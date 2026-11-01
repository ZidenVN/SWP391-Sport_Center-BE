package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.SubjectRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubjectResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.SportClassRepository;
import com.fptu.swp391.sportscentermanager.repository.SubjectRepository;
import com.fptu.swp391.sportscentermanager.service.SubjectService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.fptu.swp391.sportscentermanager.dto.SubjectResponseDTO.*;
import com.fptu.swp391.sportscentermanager.enums.AreaAccess;
import com.fptu.swp391.sportscentermanager.repository.MembershipPackageRepository;
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final SportClassRepository sportClassRepository;
    private final MembershipPackageRepository packageRepository;
    @Override
    public List<Subject> getAllSubjects() {
        return subjectRepository.findByStatus("ACTIVE");
    }

    @Override
    public Subject getSubjectById(Long id) {
        return subjectRepository.findBySubjectIdAndStatus(id, "ACTIVE").orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    @Override
    @Transactional
    public SubjectResponseDTO createSubject(SubjectRequestDTO subjectRequestDTO) {
            Subject newSubject = Subject.builder()
                .subjectName(subjectRequestDTO.getSubjectName())
                .description(subjectRequestDTO.getDescription())
                .areaAccess(subjectRequestDTO.getAreaAccess() != null ? subjectRequestDTO.getAreaAccess() : AreaAccess.OPEN)
                .build();
            Subject savedSubject = subjectRepository.save(newSubject);

            return SubjectResponseDTO.builder()
                .subjectId(savedSubject.getSubjectId())
                .subjectName(savedSubject.getSubjectName())
                .description(savedSubject.getDescription())
                .areaAccess(savedSubject.getAreaAccess())
                .build();

    }

    @Override
    public SubjectResponseDTO updateSubject(Long id, SubjectRequestDTO requestDTO) {
       Subject existingSubject = getSubjectById(id);
       existingSubject.setSubjectName(requestDTO.getSubjectName());
       existingSubject.setDescription(requestDTO.getDescription());

        // BR-SUBJ-03: đổi OPEN -> CLASS_ONLY khi còn gói FREE đang bán thì chặn
        AreaAccess newAccess = requestDTO.getAreaAccess();
        if (newAccess != null && newAccess != existingSubject.getAreaAccess()) {
            if (newAccess == AreaAccess.CLASS_ONLY && packageRepository.existsActiveFreeBySubject(id)) {
                throw new AppException(ErrorCode.SUBJECT_HAS_ACTIVE_FREE_PACKAGE);
            }
            existingSubject.setAreaAccess(newAccess);
        }

       Subject updatedSubject = subjectRepository.save(existingSubject);
       return SubjectResponseDTO.builder()
           .subjectId(updatedSubject.getSubjectId())
           .subjectName(updatedSubject.getSubjectName())
           .description(updatedSubject.getDescription())
           .areaAccess(updatedSubject.getAreaAccess())
           .build();
    }

    @Override
    public void deleteSubjectById(Long id) {
        Subject subject = getSubjectById(id);
        if (sportClassRepository.existsBySubject_SubjectIdAndStatusAndStartTimeAfter(id, "OPENING", LocalDateTime.now())){
            throw new AppException(ErrorCode.SUBJECT_IN_USE);
        }
        // BR-PKG-08: còn gói đang bán áp dụng cho môn thì chặn
        if (packageRepository.existsActiveBySubject(id)) {
            throw new AppException(ErrorCode.SUBJECT_HAS_ACTIVE_PACKAGE);
        }
        subjectRepository.delete(subject);
    }
}
