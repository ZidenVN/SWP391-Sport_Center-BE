package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.SportClassRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SportClassResponseDTO;
import com.fptu.swp391.sportscentermanager.service.SportClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.fptu.swp391.sportscentermanager.dto.ClassCancelRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class SportClassController {
    private final SportClassService sportClassService;

    @LogAudit(actionType = "CREATE", targetEntity = "SportClass")
    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SportClassResponseDTO> createClass(@Valid @RequestPart("data") SportClassRequestDTO requestDTO, @RequestPart(value = "image", required = false) MultipartFile image) {
        return   ResponseEntity.ok(sportClassService.createClass(requestDTO, image));
    }

    @PreAuthorize("hasAuthority('VIEW_CLASS')")
    @GetMapping
    public ResponseEntity<List<SportClassResponseDTO>> getAllClasses(){
        return ResponseEntity.ok(sportClassService.getAllClasses());
    }

    // BE-15: tìm lớp cho Calendar / Class catalog. Mọi tham số đều không bắt buộc.
    // VD: /api/v1/classes/search?fromDate=2026-10-20&toDate=2026-10-26&subjectId=1&available=true&page=0&size=20
    @PreAuthorize("hasAuthority('VIEW_CLASS')")
    @GetMapping("/search")
    public ResponseEntity<Page<SportClassResponseDTO>> searchClasses(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(required = false) Long subjectId,
        @RequestParam(required = false) Long coachId,
        @RequestParam(required = false) Long roomId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Boolean available,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(sportClassService.searchClasses(fromDate, toDate, subjectId, coachId, roomId,
            status, available, page, size));
    }

    @PreAuthorize("hasAuthority('VIEW_CLASS')")
    @GetMapping("/{id}")
    public ResponseEntity<SportClassResponseDTO> getClassById(@PathVariable Long id){
        return ResponseEntity.ok(sportClassService.getSportClassById(id));
    }

    @LogAudit(actionType = "ASSIGN_COACH", targetEntity = "SportClass")
    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @PutMapping("/{classId}/coach/{coachId}")
    public ResponseEntity<SportClassResponseDTO> assignCoachToClass(@PathVariable Long classId, @PathVariable Long coachId){
        return ResponseEntity.ok(sportClassService.assignCoachToClass(classId, coachId));
    }

    @LogAudit(actionType = "UPDATE", targetEntity = "SportClass")
    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @PutMapping(value = "/{id}", consumes =  MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SportClassResponseDTO> updateClass(@PathVariable Long id,
                                                             @Valid @RequestPart("data") SportClassRequestDTO requestDTO,
                                                             @RequestPart(value = "image", required = false) MultipartFile image){
        return ResponseEntity.ok(sportClassService.updateClass(id, requestDTO, image));
    }

    // BE-16: hủy lớp -> hủy các đăng ký + gửi thông báo cho học viên
    @LogAudit(actionType = "CANCEL", targetEntity = "SportClass")
    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<SportClassResponseDTO> cancelClass(@PathVariable Long id,
                                                             @Valid @RequestBody(required = false) ClassCancelRequestDTO requestDTO) {
        return ResponseEntity.ok(sportClassService.cancelClass(id, requestDTO != null ? requestDTO.getReason() : null));
    }

    // BE-16: đánh dấu lớp đã diễn ra xong
    @LogAudit(actionType = "COMPLETE", targetEntity = "SportClass")
    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<SportClassResponseDTO> completeClass(@PathVariable Long id) {
        return ResponseEntity.ok(sportClassService.completeClass(id));
    }

    @LogAudit(actionType = "DELETE", targetEntity = "SportClass")
    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteClass(@PathVariable Long id){
        sportClassService.deleteClass(id);
        return ResponseEntity.ok("Đã xóa lớp học thành công!");
    }


}
