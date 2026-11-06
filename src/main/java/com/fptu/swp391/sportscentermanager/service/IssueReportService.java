package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.IssueReportCreateDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportEscalateDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportHandleDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.enums.IssueReportCategory;
import com.fptu.swp391.sportscentermanager.enums.IssueReportDesk;
import com.fptu.swp391.sportscentermanager.enums.IssueReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IssueReportService {

    IssueReportResponseDTO createReport(User currentUser, IssueReportCreateDTO dto, MultipartFile image);

    List<IssueReportResponseDTO> getMyReports(User currentUser);

    IssueReportResponseDTO getReportById(Long reportId, User currentUser);

    Page<IssueReportResponseDTO> searchReports(User currentUser,
                                              IssueReportDesk desk,
                                              IssueReportStatus status,
                                              IssueReportCategory category,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              String keyword,
                                              int page,
                                              int size);

    Map<IssueReportStatus, Long> countReportsByStatus(User currentUser,
                                                      IssueReportDesk desk,
                                                      IssueReportCategory category,
                                                      LocalDate fromDate,
                                                      LocalDate toDate,
                                                      String keyword);

    IssueReportResponseDTO cancelReport(Long reportId, User currentUser);

    IssueReportResponseDTO handleReport(Long reportId, User currentUser, IssueReportHandleDTO dto);

    IssueReportResponseDTO escalateReport(Long reportId, User currentUser, IssueReportEscalateDTO dto);
}
