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
import com.fptu.swp391.sportscentermanager.service.ImageStorageService;
import org.springframework.web.multipart.MultipartFile;
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final SportClassRepository sportClassRepository;
    private final MembershipPackageRepository packageRepository;
    private final ImageStorageService imageStorageService;
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
    public SubjectResponseDTO createSubject(SubjectRequestDTO subjectRequestDTO, MultipartFile image) {
            // ảnh bắt buộc khi tạo môn: upload trước, lỗi thì không lưu môn
            String imageUrl = imageStorageService.uploadSubjectImage(image);
            Subject newSubject = Subject.builder()
                .subjectName(subjectRequestDTO.getSubjectName())
                .description(subjectRequestDTO.getDescription())
                .imageUrl(imageUrl)
                .areaAccess(subjectRequestDTO.getAreaAccess() != null ? subjectRequestDTO.getAreaAccess() : AreaAccess.OPEN)
                .build();
            return toResponse(subjectRepository.save(newSubject));
    }

    @Override
    public SubjectResponseDTO updateSubject(Long id, SubjectRequestDTO requestDTO, MultipartFile image) {
       Subject existingSubject = getSubjectById(id);
       existingSubject.setSubjectName(requestDTO.getSubjectName());
       existingSubject.setDescription(requestDTO.getDescription());

        // BR-SUBJ-03: đổi OPEN -> CLASS_ONLY khi còn gói FREE đang bán thì chặn
        AreaAccess newAccess = requestDTO.getAreaAccess();
        if (newAccess != null && newAccess != existingSubject.getAreaAccess()) {
            existingSubject.setAreaAccess(newAccess);
        }

        // sửa môn: không gửi ảnh thì giữ ảnh cũ
        if (image != null && !image.isEmpty()) {
            existingSubject.setImageUrl(imageStorageService.uploadSubjectImage(image));
        }

       return toResponse(subjectRepository.save(existingSubject));
    }

    private SubjectResponseDTO toResponse(Subject subject) {
        return SubjectResponseDTO.builder()
            .subjectId(subject.getSubjectId())
            .subjectName(subject.getSubjectName())
            .description(subject.getDescription())
            .imageUrl(subject.getImageUrl())
            .areaAccess(subject.getAreaAccess())
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
