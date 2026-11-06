package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.IssueReportCreateDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportEscalateDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportHandleDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportResponseDTO;
import com.fptu.swp391.sportscentermanager.enums.IssueReportCategory;
import com.fptu.swp391.sportscentermanager.enums.IssueReportDesk;
import com.fptu.swp391.sportscentermanager.enums.IssueReportStatus;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.IssueReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/issue-reports")
@RequiredArgsConstructor
public class IssueReportController {

    private final IssueReportService issueReportService;

    // 1a. Gửi báo cáo kèm ảnh (Multipart Form Data)
    @LogAudit(actionType = "CREATE", targetEntity = "IssueReport")
    @PreAuthorize("hasAuthority('SUBMIT_ISSUE_REPORT')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IssueReportResponseDTO> createReportMultipart(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestPart("data") IssueReportCreateDTO dto,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(issueReportService.createReport(principal.getUser(), dto, image));
    }

    // 1b. Gửi báo cáo dạng JSON thuần (không kèm ảnh)
    @LogAudit(actionType = "CREATE", targetEntity = "IssueReport")
    @PreAuthorize("hasAuthority('SUBMIT_ISSUE_REPORT')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IssueReportResponseDTO> createReportJson(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody IssueReportCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(issueReportService.createReport(principal.getUser(), dto, null));
    }

    // 2. Lấy danh sách báo cáo của chính mình (Hội viên, HLV, Lễ tân xem report mình gửi)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<List<IssueReportResponseDTO>> getMyReports(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(issueReportService.getMyReports(principal.getUser()));
    }

    // 3. Lễ tân / Manager xem danh sách báo cáo cần xử lý (hỗ trợ phân quyền bàn, filter, phân trang)
    @PreAuthorize("hasAuthority('HANDLE_ISSUE_REPORT')")
    @GetMapping
    public ResponseEntity<Page<IssueReportResponseDTO>> searchReports(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) IssueReportDesk desk,
            @RequestParam(required = false) IssueReportStatus status,
            @RequestParam(required = false) IssueReportCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(issueReportService.searchReports(principal.getUser(), desk, status, category,
                fromDate, toDate, keyword, page, size));
    }

    // Đếm báo cáo theo từng trạng thái, dùng cùng bộ lọc với danh sách (trừ status)
    @PreAuthorize("hasAuthority('HANDLE_ISSUE_REPORT')")
    @GetMapping("/stats")
    public ResponseEntity<Map<IssueReportStatus, Long>> countReportsByStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) IssueReportDesk desk,
            @RequestParam(required = false) IssueReportCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(issueReportService.countReportsByStatus(principal.getUser(), desk, category,
                fromDate, toDate, keyword));
    }

    // 4. Xem chi tiết 1 báo cáo sự cố
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<IssueReportResponseDTO> getReportById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(issueReportService.getReportById(id, principal.getUser()));
    }

    // 5. Người gửi tự huỷ báo cáo khi còn ở trạng thái OPEN
    @LogAudit(actionType = "CANCEL", targetEntity = "IssueReport")
    @PreAuthorize("hasAuthority('SUBMIT_ISSUE_REPORT')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<IssueReportResponseDTO> cancelReport(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(issueReportService.cancelReport(id, principal.getUser()));
    }

    // 6. Lễ tân / Manager xử lý báo cáo (đổi trạng thái IN_PROGRESS, RESOLVED, REJECTED kèm phản hồi)
    @LogAudit(actionType = "UPDATE_STATUS", targetEntity = "IssueReport")
    @PreAuthorize("hasAuthority('HANDLE_ISSUE_REPORT')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<IssueReportResponseDTO> handleReport(
            @PathVariable Long id,
            @Valid @RequestBody IssueReportHandleDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(issueReportService.handleReport(id, principal.getUser(), dto));
    }

    // 7. Lễ tân chuyển báo cáo lên Manager
    @LogAudit(actionType = "ESCALATE", targetEntity = "IssueReport")
    @PreAuthorize("hasAuthority('HANDLE_ISSUE_REPORT')")
    @PatchMapping("/{id}/escalate")
    public ResponseEntity<IssueReportResponseDTO> escalateReport(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) IssueReportEscalateDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(issueReportService.escalateReport(id, principal.getUser(), dto));
    }
}
