package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.SubjectRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubjectResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class
SubjectController {
    private final SubjectService subjectService;

    @GetMapping
    public ResponseEntity<List<Subject>> getAllSubjects() {
        List<Subject> subjects = subjectService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subject> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @LogAudit(actionType = "CREATE", targetEntity = "Subject")
    @PreAuthorize("hasAuthority('MANAGE_SUBJECT')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubjectResponseDTO> createSubject(@Valid @RequestPart("data") SubjectRequestDTO subjectRequestDTO,
                                                            @RequestPart(value = "image", required = false) MultipartFile image) {
        return  ResponseEntity.ok(subjectService.createSubject(subjectRequestDTO, image));
    }

    @LogAudit(actionType = "UPDATE", targetEntity = "Subject")
    @PreAuthorize("hasAuthority('MANAGE_SUBJECT')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubjectResponseDTO> updateSubject(@PathVariable Long id,
                                                            @Valid @RequestPart("data") SubjectRequestDTO subjectRequestDTO,
                                                            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(subjectService.updateSubject(id, subjectRequestDTO, image));
    }

    @LogAudit(actionType = "DELETE", targetEntity = "Subject")
    @PreAuthorize("hasAuthority('MANAGE_SUBJECT')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubjectById(id);
        return ResponseEntity.ok("Đã xóa bộ môn thành công!");
    }
}
