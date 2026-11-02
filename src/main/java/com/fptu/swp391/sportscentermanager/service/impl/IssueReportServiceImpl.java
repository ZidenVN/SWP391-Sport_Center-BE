package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.IssueReportCreateDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportEscalateDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportHandleDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.IssueReport;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.IssueReportCategory;
import com.fptu.swp391.sportscentermanager.enums.IssueReportDesk;
import com.fptu.swp391.sportscentermanager.enums.IssueReportStatus;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.IssueReportRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.service.ImageStorageService;
import com.fptu.swp391.sportscentermanager.service.IssueReportService;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class IssueReportServiceImpl implements IssueReportService {

    private final IssueReportRepository issueReportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ImageStorageService imageStorageService;

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    @Transactional
    public IssueReportResponseDTO createReport(User currentUser, IssueReportCreateDTO dto, MultipartFile image) {
        String userRole = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";
        String categoryRole = dto.getCategory() != null ? dto.getCategory().getRole() : "";

        boolean roleAllowed = userRole.equalsIgnoreCase(categoryRole);
        if (!roleAllowed) {
            throw new AppException(ErrorCode.REPORT_CATEGORY_NOT_ALLOWED);
        }

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = imageStorageService.uploadReportImage(image);
        }

        IssueReportDesk initialDesk = dto.getCategory().getDesk();
        LocalDateTime now = LocalDateTime.now();

        IssueReport report = IssueReport.builder()
                .reporter(currentUser)
                .category(dto.getCategory())
                .title(dto.getTitle().trim())
                .content(dto.getContent().trim())
                .imageUrl(imageUrl)
                .status(IssueReportStatus.OPEN)
                .desk(initialDesk)
                .createdAt(now)
                .updatedAt(now)
                .build();

        IssueReport saved = issueReportRepository.save(report);

        // Bắn thông báo Realtime theo Desk
        try {
            if (initialDesk == IssueReportDesk.RECEPTION) {
                List<User> receptionists = userRepository.findByRole_RoleNameAndStatus("RECEPTIONIST", UserStatus.ACTIVE);
                if (!receptionists.isEmpty()) {
                    notificationService.notifyUsers(receptionists, "Sự cố mới cần xử lý",
                            "Hội viên " + currentUser.getFirstName() + " gửi sự cố: " + saved.getTitle(),
                            "ISSUE_REPORT_CREATED", currentUser);
                }
            } else if (initialDesk == IssueReportDesk.MANAGER) {
                List<User> managers = userRepository.findByRole_RoleNameAndStatus("CENTER_MANAGER", UserStatus.ACTIVE);
                if (!managers.isEmpty()) {
                    notificationService.notifyUsers(managers, "Sự cố mới cần xử lý",
                            userRole + " " + currentUser.getFirstName() + " gửi sự cố: " + saved.getTitle(),
                            "ISSUE_REPORT_CREATED", currentUser);
                }
            }
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo tạo sự cố: {}", e.getMessage());
        }

        return toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueReportResponseDTO> getMyReports(User currentUser) {
        return issueReportRepository.findByReporter_UserIdOrderByCreatedAtDesc(currentUser.getUserId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public IssueReportResponseDTO getReportById(Long reportId, User currentUser) {
        IssueReport report = issueReportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        String userRole = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";
        boolean isReporter = report.getReporter().getUserId().equals(currentUser.getUserId());
        boolean isManager = "CENTER_MANAGER".equalsIgnoreCase(userRole);
        boolean isReceptionist = "RECEPTIONIST".equalsIgnoreCase(userRole);

        if (!isReporter && !isManager && !(isReceptionist && report.getDesk() == IssueReportDesk.RECEPTION)) {
            throw new AppException(ErrorCode.REPORT_ACCESS_DENIED);
        }

        return toResponseDTO(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IssueReportResponseDTO> searchReports(User currentUser,
                                                      IssueReportDesk desk,
                                                      IssueReportStatus status,
                                                      IssueReportCategory category,
                                                      LocalDate fromDate,
                                                      LocalDate toDate,
                                                      String keyword,
                                                      int page,
                                                      int size) {
        String userRole = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";

        // Kiểm soát phân quyền theo bàn xử lý
        if ("RECEPTIONIST".equalsIgnoreCase(userRole)) {
            if (desk != null && desk != IssueReportDesk.RECEPTION) {
                throw new AppException(ErrorCode.REPORT_DESK_ACCESS_DENIED);
            }
            desk = IssueReportDesk.RECEPTION;
        } else if (!"CENTER_MANAGER".equalsIgnoreCase(userRole)) {
            throw new AppException(ErrorCode.REPORT_ACCESS_DENIED);
        }

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<IssueReport> result = issueReportRepository.searchReports(desk, status, category, from, to, kw, pageable);
        return result.map(this::toResponseDTO);
    }

    @Override
    @Transactional
    public IssueReportResponseDTO cancelReport(Long reportId, User currentUser) {
        IssueReport report = issueReportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        if (!report.getReporter().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.REPORT_ACCESS_DENIED);
        }

        if (report.getStatus() != IssueReportStatus.OPEN) {
            throw new AppException(ErrorCode.REPORT_CANNOT_CANCEL);
        }

        report.setStatus(IssueReportStatus.CANCELLED);
        report.setUpdatedAt(LocalDateTime.now());
        IssueReport saved = issueReportRepository.save(report);

        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public IssueReportResponseDTO handleReport(Long reportId, User currentUser, IssueReportHandleDTO dto) {
        IssueReport report = issueReportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() == IssueReportStatus.RESOLVED
                || report.getStatus() == IssueReportStatus.REJECTED
                || report.getStatus() == IssueReportStatus.CANCELLED) {
            throw new AppException(ErrorCode.REPORT_ALREADY_CLOSED);
        }

        String userRole = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";
        if ("RECEPTIONIST".equalsIgnoreCase(userRole) && report.getDesk() != IssueReportDesk.RECEPTION) {
            throw new AppException(ErrorCode.REPORT_DESK_ACCESS_DENIED);
        }

        report.setStatus(dto.getStatus());
        if (dto.getResponse() != null && !dto.getResponse().isBlank()) {
            report.setResponse(dto.getResponse().trim());
        }
        report.setHandledBy(currentUser);
        report.setUpdatedAt(LocalDateTime.now());

        IssueReport saved = issueReportRepository.save(report);

        // Bắn thông báo Realtime cho người gửi
        try {
            String msg = "Báo cáo sự cố \"" + saved.getTitle() + "\" của bạn đã chuyển sang: " + saved.getStatus().name()
                    + (saved.getResponse() != null ? ". Phản hồi: " + saved.getResponse() : "");
            notificationService.notifyUsers(List.of(saved.getReporter()), "Cập nhật báo cáo sự cố",
                    msg, "ISSUE_REPORT_UPDATED", currentUser);
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo cập nhật sự cố: {}", e.getMessage());
        }

        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public IssueReportResponseDTO escalateReport(Long reportId, User currentUser, IssueReportEscalateDTO dto) {
        IssueReport report = issueReportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getDesk() != IssueReportDesk.RECEPTION) {
            throw new AppException(ErrorCode.REPORT_CANNOT_ESCALATE);
        }

        if (report.getStatus() == IssueReportStatus.RESOLVED
                || report.getStatus() == IssueReportStatus.REJECTED
                || report.getStatus() == IssueReportStatus.CANCELLED) {
            throw new AppException(ErrorCode.REPORT_ALREADY_CLOSED);
        }

        report.setDesk(IssueReportDesk.MANAGER);
        report.setHandledBy(currentUser);
        report.setUpdatedAt(LocalDateTime.now());

        String reasonNote = dto != null && dto.getReason() != null && !dto.getReason().isBlank()
                ? dto.getReason().trim()
                : "Lễ tân chuyển tiếp lên Quản lý xử lý";
        String existingResp = report.getResponse();
        report.setResponse((existingResp != null && !existingResp.isBlank() ? existingResp + " | " : "")
                + "[Chuyển lên Manager bởi " + currentUser.getFirstName() + "]: " + reasonNote);

        IssueReport saved = issueReportRepository.save(report);

        // Bắn thông báo Realtime cho Manager và Reporter
        try {
            List<User> managers = userRepository.findByRole_RoleNameAndStatus("CENTER_MANAGER", UserStatus.ACTIVE);
            if (!managers.isEmpty()) {
                notificationService.notifyUsers(managers, "Sự cố chuyển tiếp lên Quản lý",
                        "Báo cáo \"" + saved.getTitle() + "\" đã được Lễ tân chuyển lên. Lý do: " + reasonNote,
                        "ISSUE_REPORT_ESCALATED", currentUser);
            }
            notificationService.notifyUsers(List.of(saved.getReporter()), "Báo cáo sự cố đã chuyển tiếp",
                    "Báo cáo \"" + saved.getTitle() + "\" của bạn đã được chuyển lên cấp Quản lý để xử lý.",
                    "ISSUE_REPORT_ESCALATED", currentUser);
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo escalate sự cố: {}", e.getMessage());
        }

        return toResponseDTO(saved);
    }

    private IssueReportResponseDTO toResponseDTO(IssueReport r) {
        User rep = r.getReporter();
        User hb = r.getHandledBy();
        return IssueReportResponseDTO.builder()
                .reportId(r.getReportId())
                .reporterId(rep != null ? rep.getUserId() : null)
                .reporterName(rep != null ? rep.getFirstName() + " " + rep.getLastName() : "N/A")
                .reporterEmail(rep != null ? rep.getEmail() : null)
                .reporterPhone(rep != null ? rep.getPhone() : null)
                .reporterRole(rep != null && rep.getRole() != null ? rep.getRole().getRoleName() : null)
                .category(r.getCategory())
                .categoryLabel(r.getCategory() != null ? r.getCategory().getLabel() : null)
                .title(r.getTitle())
                .content(r.getContent())
                .imageUrl(r.getImageUrl())
                .status(r.getStatus())
                .desk(r.getDesk())
                .handledById(hb != null ? hb.getUserId() : null)
                .handledByName(hb != null ? hb.getFirstName() + " " + hb.getLastName() : null)
                .response(r.getResponse())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
