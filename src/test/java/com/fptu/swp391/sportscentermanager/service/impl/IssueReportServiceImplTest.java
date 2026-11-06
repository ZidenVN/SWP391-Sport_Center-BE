package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.IssueReportCreateDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportEscalateDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportHandleDTO;
import com.fptu.swp391.sportscentermanager.dto.IssueReportResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.IssueReport;
import com.fptu.swp391.sportscentermanager.entity.Role;
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
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueReportServiceImplTest {

    @Mock
    private IssueReportRepository issueReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private IssueReportServiceImpl issueReportService;

    private User memberUser;
    private User coachUser;
    private User receptionistUser;
    private User managerUser;

    @BeforeEach
    void setUp() {
        Role memberRole = Role.builder().roleId(1L).roleName("MEMBER").build();
        Role coachRole = Role.builder().roleId(2L).roleName("COACH").build();
        Role receptionistRole = Role.builder().roleId(3L).roleName("RECEPTIONIST").build();
        Role managerRole = Role.builder().roleId(4L).roleName("CENTER_MANAGER").build();

        memberUser = User.builder()
                .userId(10L)
                .firstName("Van A")
                .lastName("Nguyen")
                .email("member@example.com")
                .role(memberRole)
                .build();

        coachUser = User.builder()
                .userId(20L)
                .firstName("Van B")
                .lastName("Tran")
                .email("coach@example.com")
                .role(coachRole)
                .build();

        receptionistUser = User.builder()
                .userId(30L)
                .firstName("Thi C")
                .lastName("Le")
                .email("receptionist@example.com")
                .role(receptionistRole)
                .build();

        managerUser = User.builder()
                .userId(40L)
                .firstName("Van D")
                .lastName("Pham")
                .email("manager@example.com")
                .role(managerRole)
                .build();
    }

    @Test
    @DisplayName("Tạo báo cáo thành công - Member gửi sự cố cơ sở vật chất (Desk RECEPTION)")
    void createReport_success_member() {
        IssueReportCreateDTO dto = IssueReportCreateDTO.builder()
                .category(IssueReportCategory.MEMBER_FACILITY)
                .title("Máy chạy bộ số 3 bị hỏng")
                .content("Băng tải không quay khi bấm start")
                .build();

        when(issueReportRepository.save(any(IssueReport.class))).thenAnswer(inv -> {
            IssueReport r = inv.getArgument(0);
            r.setReportId(100L);
            return r;
        });

        when(userRepository.findByRole_RoleNameAndStatus("RECEPTIONIST", UserStatus.ACTIVE))
                .thenReturn(List.of(receptionistUser));

        IssueReportResponseDTO res = issueReportService.createReport(memberUser, dto, null);

        assertNotNull(res);
        assertEquals(100L, res.getReportId());
        assertEquals("Máy chạy bộ số 3 bị hỏng", res.getTitle());
        assertEquals(IssueReportCategory.MEMBER_FACILITY, res.getCategory());
        assertEquals(IssueReportDesk.RECEPTION, res.getDesk());
        assertEquals(IssueReportStatus.OPEN, res.getStatus());
        assertEquals(10L, res.getReporterId());

        verify(notificationService).notifyUsers(eq(List.of(receptionistUser)), anyString(), anyString(), eq("ISSUE_REPORT_CREATED"), eq(memberUser));
    }

    @Test
    @DisplayName("Tạo báo cáo thành công kèm hình ảnh")
    void createReport_success_withImage() {
        MockMultipartFile file = new MockMultipartFile("image", "broken.jpg", "image/jpeg", "fake-img".getBytes());
        IssueReportCreateDTO dto = IssueReportCreateDTO.builder()
                .category(IssueReportCategory.MEMBER_HYGIENE)
                .title("Khu vực phòng tắm bẩn")
                .content("Cần dọn dẹp phòng thay đồ nam")
                .build();

        when(imageStorageService.uploadReportImage(file)).thenReturn("https://res.cloudinary.com/demo/image/upload/broken.jpg");
        when(issueReportRepository.save(any(IssueReport.class))).thenAnswer(inv -> {
            IssueReport r = inv.getArgument(0);
            r.setReportId(101L);
            return r;
        });

        IssueReportResponseDTO res = issueReportService.createReport(memberUser, dto, file);

        assertNotNull(res);
        assertEquals("https://res.cloudinary.com/demo/image/upload/broken.jpg", res.getImageUrl());
        verify(imageStorageService).uploadReportImage(file);
    }

    @Test
    @DisplayName("Tạo báo cáo - HLV gửi sự cố thiết bị phòng (Desk MANAGER)")
    void createReport_success_coach_managerDesk() {
        IssueReportCreateDTO dto = IssueReportCreateDTO.builder()
                .category(IssueReportCategory.COACH_ROOM_EQUIPMENT)
                .title("Loa bluetooth phòng Yoga 1 bị rè")
                .content("Âm thanh bị rè không thể mở nhạc tập thiền")
                .build();

        when(issueReportRepository.save(any(IssueReport.class))).thenAnswer(inv -> {
            IssueReport r = inv.getArgument(0);
            r.setReportId(102L);
            return r;
        });
        when(userRepository.findByRole_RoleNameAndStatus("CENTER_MANAGER", UserStatus.ACTIVE))
                .thenReturn(List.of(managerUser));

        IssueReportResponseDTO res = issueReportService.createReport(coachUser, dto, null);

        assertNotNull(res);
        assertEquals(IssueReportDesk.MANAGER, res.getDesk());
        verify(notificationService).notifyUsers(eq(List.of(managerUser)), anyString(), anyString(), eq("ISSUE_REPORT_CREATED"), eq(coachUser));
    }

    @Test
    @DisplayName("Tạo báo cáo thất bại - Sai danh mục cho role của user (Member chọn category của Coach)")
    void createReport_categoryNotAllowed() {
        IssueReportCreateDTO dto = IssueReportCreateDTO.builder()
                .category(IssueReportCategory.COACH_ROOM_EQUIPMENT)
                .title("Sai role")
                .content("Nội dung test")
                .build();

        AppException ex = assertThrows(AppException.class, () -> issueReportService.createReport(memberUser, dto, null));
        assertEquals(ErrorCode.REPORT_CATEGORY_NOT_ALLOWED, ex.getErrorCode());
        verify(issueReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo báo cáo thất bại - Manager không có thẩm quyền gửi report (chỉ xử lý)")
    void createReport_managerNotAllowedToSubmit() {
        IssueReportCreateDTO dto = IssueReportCreateDTO.builder()
                .category(IssueReportCategory.MEMBER_FACILITY)
                .title("Manager gửi báo cáo")
                .content("Nội dung manager")
                .build();

        AppException ex = assertThrows(AppException.class,
                () -> issueReportService.createReport(managerUser, dto, null));
        assertEquals(ErrorCode.REPORT_CATEGORY_NOT_ALLOWED, ex.getErrorCode());
        verify(issueReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lấy danh sách báo cáo của bản thân (getMyReports)")
    void getMyReports_success() {
        IssueReport r = IssueReport.builder()
                .reportId(200L)
                .reporter(memberUser)
                .category(IssueReportCategory.MEMBER_FACILITY)
                .title("Báo cáo 1")
                .content("Nội dung 1")
                .desk(IssueReportDesk.RECEPTION)
                .status(IssueReportStatus.OPEN)
                .build();

        when(issueReportRepository.findByReporter_UserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(r));

        List<IssueReportResponseDTO> list = issueReportService.getMyReports(memberUser);
        assertEquals(1, list.size());
        assertEquals(200L, list.get(0).getReportId());
    }

    @Test
    @DisplayName("Xem chi tiết báo cáo - Người gửi xem báo cáo của chính mình")
    void getReportById_success_reporter() {
        IssueReport r = IssueReport.builder()
                .reportId(300L)
                .reporter(memberUser)
                .category(IssueReportCategory.MEMBER_FACILITY)
                .title("Báo cáo test")
                .desk(IssueReportDesk.RECEPTION)
                .status(IssueReportStatus.OPEN)
                .build();

        when(issueReportRepository.findById(300L)).thenReturn(Optional.of(r));

        IssueReportResponseDTO res = issueReportService.getReportById(300L, memberUser);
        assertNotNull(res);
        assertEquals(300L, res.getReportId());
    }

    @Test
    @DisplayName("Xem chi tiết báo cáo - Lễ tân xem báo cáo ở bàn RECEPTION thành công")
    void getReportById_success_receptionist() {
        IssueReport r = IssueReport.builder()
                .reportId(301L)
                .reporter(memberUser)
                .category(IssueReportCategory.MEMBER_FACILITY)
                .title("Báo cáo test")
                .desk(IssueReportDesk.RECEPTION)
                .status(IssueReportStatus.OPEN)
                .build();

        when(issueReportRepository.findById(301L)).thenReturn(Optional.of(r));

        IssueReportResponseDTO res = issueReportService.getReportById(301L, receptionistUser);
        assertNotNull(res);
    }

    @Test
    @DisplayName("Xem chi tiết báo cáo - Lễ tân bị chặn khi xem báo cáo ở bàn MANAGER")
    void getReportById_accessDenied_receptionist_managerDesk() {
        IssueReport r = IssueReport.builder()
                .reportId(302L)
                .reporter(coachUser)
                .category(IssueReportCategory.COACH_ROOM_EQUIPMENT)
                .title("Báo cáo thuộc bàn Manager")
                .desk(IssueReportDesk.MANAGER)
                .status(IssueReportStatus.OPEN)
                .build();

        when(issueReportRepository.findById(302L)).thenReturn(Optional.of(r));

        AppException ex = assertThrows(AppException.class, () -> issueReportService.getReportById(302L, receptionistUser));
        assertEquals(ErrorCode.REPORT_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Xem chi tiết báo cáo - Báo cáo không tồn tại -> REPORT_NOT_FOUND")
    void getReportById_notFound() {
        when(issueReportRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> issueReportService.getReportById(999L, memberUser));
        assertEquals(ErrorCode.REPORT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("Tìm kiếm báo cáo - Manager tìm kiếm theo bộ lọc thành công")
    void searchReports_success_manager() {
        IssueReport r = IssueReport.builder()
                .reportId(400L)
                .reporter(memberUser)
                .category(IssueReportCategory.MEMBER_FACILITY)
                .desk(IssueReportDesk.MANAGER)
                .status(IssueReportStatus.IN_PROGRESS)
                .build();

        Page<IssueReport> page = new PageImpl<>(List.of(r));
        when(issueReportRepository.searchReports(any(), eq(false), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<IssueReportResponseDTO> result = issueReportService.searchReports(
                managerUser, IssueReportDesk.MANAGER, IssueReportStatus.IN_PROGRESS,
                IssueReportCategory.MEMBER_FACILITY, null, null, null, 0, 10);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Đếm theo trạng thái - Status không có báo cáo vẫn trả về 0")
    void countReportsByStatus_fillsMissingStatusWithZero() {
        List<Object[]> rows = Collections.singletonList(new Object[] {IssueReportStatus.OPEN, 3L});
        when(issueReportRepository.countByStatus(any(), eq(false), any(), any(), any(), any())).thenReturn(rows);

        Map<IssueReportStatus, Long> result = issueReportService.countReportsByStatus(
                managerUser, null, null, null, null, null);

        assertEquals(IssueReportStatus.values().length, result.size());
        assertEquals(3L, result.get(IssueReportStatus.OPEN));
        assertEquals(0L, result.get(IssueReportStatus.RESOLVED));
    }

    @Test
    @DisplayName("Đếm theo trạng thái - Lễ tân chỉ đếm ở bàn RECEPTION và báo cáo đã chuyển lên")
    void countReportsByStatus_receptionistScopedToReceptionDesk() {
        when(issueReportRepository.countByStatus(eq(IssueReportDesk.RECEPTION), eq(true), any(), any(), any(), any()))
                .thenReturn(List.of());

        Map<IssueReportStatus, Long> result = issueReportService.countReportsByStatus(
                receptionistUser, null, null, null, null, null);

        assertEquals(0L, result.get(IssueReportStatus.OPEN));
    }

    @Test
    @DisplayName("Đếm theo trạng thái - Lễ tân không được đếm bàn MANAGER")
    void countReportsByStatus_forbidden_receptionistAttemptsManagerDesk() {
        AppException ex = assertThrows(AppException.class, () -> issueReportService.countReportsByStatus(
                receptionistUser, IssueReportDesk.MANAGER, null, null, null, null));

        assertEquals(ErrorCode.REPORT_DESK_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Tìm kiếm báo cáo - Lễ tân chỉ được tìm kiếm ở bàn RECEPTION")
    void searchReports_success_receptionist() {
        Page<IssueReport> page = new PageImpl<>(Collections.emptyList());
        when(issueReportRepository.searchReports(eq(IssueReportDesk.RECEPTION), eq(true), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<IssueReportResponseDTO> result = issueReportService.searchReports(
                receptionistUser, null, null, null, null, null, null, 0, 10);

        assertNotNull(result);
        // lễ tân: bàn RECEPTION kèm các báo cáo mình đã chuyển lên (includeEscalated = true)
        verify(issueReportRepository).searchReports(eq(IssueReportDesk.RECEPTION), eq(true), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Tìm kiếm báo cáo thất bại - Lễ tân cố tình tìm bàn MANAGER")
    void searchReports_forbidden_receptionistAttemptsManagerDesk() {
        AppException ex = assertThrows(AppException.class, () ->
                issueReportService.searchReports(receptionistUser, IssueReportDesk.MANAGER, null, null, null, null, null, 0, 10));

        assertEquals(ErrorCode.REPORT_DESK_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Tìm kiếm báo cáo thất bại - Sai khoảng ngày (fromDate > toDate)")
    void searchReports_invalidDateRange() {
        LocalDate from = LocalDate.of(2026, 11, 10);
        LocalDate to = LocalDate.of(2026, 11, 1);

        AppException ex = assertThrows(AppException.class, () ->
                issueReportService.searchReports(managerUser, null, null, null, from, to, null, 0, 10));

        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    @DisplayName("Huỷ báo cáo thành công khi trạng thái còn OPEN và đúng người gửi")
    void cancelReport_success() {
        IssueReport r = IssueReport.builder()
                .reportId(500L)
                .reporter(memberUser)
                .category(IssueReportCategory.MEMBER_FACILITY)
                .status(IssueReportStatus.OPEN)
                .build();

        when(issueReportRepository.findById(500L)).thenReturn(Optional.of(r));
        when(issueReportRepository.save(any(IssueReport.class))).thenAnswer(inv -> inv.getArgument(0));

        IssueReportResponseDTO res = issueReportService.cancelReport(500L, memberUser);

        assertEquals(IssueReportStatus.CANCELLED, res.getStatus());
    }

    @Test
    @DisplayName("Huỷ báo cáo thất bại - Trạng thái đã sang IN_PROGRESS")
    void cancelReport_notOpen() {
        IssueReport r = IssueReport.builder()
                .reportId(501L)
                .reporter(memberUser)
                .status(IssueReportStatus.IN_PROGRESS)
                .build();

        when(issueReportRepository.findById(501L)).thenReturn(Optional.of(r));

        AppException ex = assertThrows(AppException.class, () -> issueReportService.cancelReport(501L, memberUser));
        assertEquals(ErrorCode.REPORT_CANNOT_CANCEL, ex.getErrorCode());
    }

    @Test
    @DisplayName("Huỷ báo cáo thất bại - Không phải người tạo báo cáo")
    void cancelReport_notOwner() {
        IssueReport r = IssueReport.builder()
                .reportId(502L)
                .reporter(memberUser)
                .status(IssueReportStatus.OPEN)
                .build();

        when(issueReportRepository.findById(502L)).thenReturn(Optional.of(r));

        AppException ex = assertThrows(AppException.class, () -> issueReportService.cancelReport(502L, coachUser));
        assertEquals(ErrorCode.REPORT_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Xử lý báo cáo thành công - Lễ tân chuyển sang RESOLVED kèm phản hồi")
    void handleReport_success_resolve() {
        IssueReport r = IssueReport.builder()
                .reportId(600L)
                .reporter(memberUser)
                .desk(IssueReportDesk.RECEPTION)
                .status(IssueReportStatus.IN_PROGRESS)
                .title("Sự cố vòi nước")
                .build();

        IssueReportHandleDTO dto = IssueReportHandleDTO.builder()
                .status(IssueReportStatus.RESOLVED)
                .response("Đã cử kỹ thuật sửa chữa xong vòi nước.")
                .build();

        when(issueReportRepository.findById(600L)).thenReturn(Optional.of(r));
        when(issueReportRepository.save(any(IssueReport.class))).thenAnswer(inv -> inv.getArgument(0));

        IssueReportResponseDTO res = issueReportService.handleReport(600L, receptionistUser, dto);

        assertEquals(IssueReportStatus.RESOLVED, res.getStatus());
        assertEquals("Đã cử kỹ thuật sửa chữa xong vòi nước.", res.getResponse());
        assertEquals(30L, res.getHandledById());
        assertNotNull(res.getHandledAt());

        verify(notificationService).notifyUsers(eq(List.of(memberUser)), anyString(), anyString(), eq("ISSUE_REPORT_UPDATED"), eq(receptionistUser));
    }

    @Test
    @DisplayName("Xử lý báo cáo thất bại - người xử lý không được chuyển sang CANCELLED hoặc OPEN")
    void handleReport_invalidStatus_rejected() {
        for (IssueReportStatus bad : List.of(IssueReportStatus.CANCELLED, IssueReportStatus.OPEN)) {
            IssueReport r = IssueReport.builder()
                    .reportId(603L)
                    .reporter(memberUser)
                    .desk(IssueReportDesk.RECEPTION)
                    .status(IssueReportStatus.IN_PROGRESS)
                    .build();
            when(issueReportRepository.findById(603L)).thenReturn(Optional.of(r));

            AppException ex = assertThrows(AppException.class, () -> issueReportService.handleReport(603L, receptionistUser,
                    IssueReportHandleDTO.builder().status(bad).build()));
            assertEquals(ErrorCode.REPORT_INVALID_STATUS, ex.getErrorCode());
            assertEquals(IssueReportStatus.IN_PROGRESS, r.getStatus());
        }
    }

    @Test
    @DisplayName("Người báo cáo xem báo cáo đã chuyển cấp: không thấy người chuyển và lý do chuyển")
    void reporterView_hidesEscalation() {
        IssueReport r = IssueReport.builder()
                .reportId(605L)
                .reporter(memberUser)
                .category(IssueReportCategory.MEMBER_FACILITY)
                .desk(IssueReportDesk.MANAGER)
                .status(IssueReportStatus.OPEN)
                .escalatedBy(receptionistUser)
                .escalateReason("Khách hay phàn nàn, cần quản lý gọi lại")
                .build();
        when(issueReportRepository.findById(605L)).thenReturn(Optional.of(r));

        IssueReportResponseDTO asMember = issueReportService.getReportById(605L, memberUser);
        IssueReportResponseDTO asManager = issueReportService.getReportById(605L, managerUser);

        assertNull(asMember.getEscalateReason());
        assertNull(asMember.getEscalatedById());
        // hội viên thấy nơi nhận ban đầu (lễ tân), không thấy báo cáo đã sang bàn quản lý
        assertEquals(IssueReportDesk.RECEPTION, asMember.getDesk());
        assertEquals(IssueReportDesk.MANAGER, asManager.getDesk());
        assertEquals("Khách hay phàn nàn, cần quản lý gọi lại", asManager.getEscalateReason());
        assertEquals(receptionistUser.getUserId(), asManager.getEscalatedById());
    }

    @Test
    @DisplayName("Đóng báo cáo (Đã xử lý / Từ chối) mà không có phản hồi: bị chặn")
    void handleReport_closingWithoutResponse_rejected() {
        for (IssueReportStatus closing : List.of(IssueReportStatus.RESOLVED, IssueReportStatus.REJECTED)) {
            IssueReport r = IssueReport.builder()
                    .reportId(606L)
                    .reporter(memberUser)
                    .desk(IssueReportDesk.RECEPTION)
                    .status(IssueReportStatus.IN_PROGRESS)
                    .response("Lễ tân đã ghi nhận")
                    .build();
            when(issueReportRepository.findById(606L)).thenReturn(Optional.of(r));

            AppException ex = assertThrows(AppException.class, () -> issueReportService.handleReport(606L, managerUser,
                    IssueReportHandleDTO.builder().status(closing).response("  ").build()));
            assertEquals(ErrorCode.REPORT_RESPONSE_REQUIRED, ex.getErrorCode());
            assertEquals(IssueReportStatus.IN_PROGRESS, r.getStatus());
        }
    }

    @Test
    @DisplayName("Gửi lại đúng trạng thái hiện tại mà không có phản hồi mới: bị chặn, không báo trùng cho người gửi")
    void handleReport_sameStatusWithoutResponse_rejected() {
        IssueReport r = IssueReport.builder()
                .reportId(609L)
                .reporter(memberUser)
                .desk(IssueReportDesk.RECEPTION)
                .status(IssueReportStatus.IN_PROGRESS)
                .build();
        when(issueReportRepository.findById(609L)).thenReturn(Optional.of(r));

        AppException ex = assertThrows(AppException.class, () -> issueReportService.handleReport(609L, receptionistUser,
                IssueReportHandleDTO.builder().status(IssueReportStatus.IN_PROGRESS).build()));

        assertEquals(ErrorCode.REPORT_STATUS_UNCHANGED, ex.getErrorCode());
        verify(notificationService, never()).notifyUsers(anyList(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Đang xử lý mà gửi thêm phản hồi mới: được phép, người gửi nhận cập nhật")
    void handleReport_sameStatusWithNewResponse_allowed() {
        IssueReport r = IssueReport.builder()
                .reportId(610L)
                .reporter(memberUser)
                .desk(IssueReportDesk.RECEPTION)
                .status(IssueReportStatus.IN_PROGRESS)
                .title("Máy lạnh không mát")
                .build();
        when(issueReportRepository.findById(610L)).thenReturn(Optional.of(r));
        when(issueReportRepository.save(any(IssueReport.class))).thenAnswer(inv -> inv.getArgument(0));

        IssueReportResponseDTO res = issueReportService.handleReport(610L, receptionistUser,
                IssueReportHandleDTO.builder().status(IssueReportStatus.IN_PROGRESS).response("Đã gọi thợ, chiều nay tới").build());

        assertEquals("Đã gọi thợ, chiều nay tới", res.getResponse());
        verify(notificationService).notifyUsers(eq(List.of(memberUser)), anyString(), anyString(), eq("ISSUE_REPORT_UPDATED"), eq(receptionistUser));
    }

    @Test
    @DisplayName("Thông báo gửi người báo cáo dùng nhãn tiếng Việt, không dùng mã trạng thái")
    void handleReport_notificationUsesVietnameseLabel() {
        IssueReport r = IssueReport.builder()
                .reportId(607L)
                .reporter(memberUser)
                .desk(IssueReportDesk.RECEPTION)
                .status(IssueReportStatus.IN_PROGRESS)
                .title("Vòi nước rò")
                .build();
        when(issueReportRepository.findById(607L)).thenReturn(Optional.of(r));
        when(issueReportRepository.save(any(IssueReport.class))).thenAnswer(inv -> inv.getArgument(0));

        issueReportService.handleReport(607L, receptionistUser,
                IssueReportHandleDTO.builder().status(IssueReportStatus.RESOLVED).response("Đã thay vòi mới").build());

        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyUsers(eq(List.of(memberUser)), anyString(), content.capture(), eq("ISSUE_REPORT_UPDATED"), eq(receptionistUser));
        assertTrue(content.getValue().contains("Đã xử lý"));
        assertFalse(content.getValue().contains("RESOLVED"));
    }

    @Test
    @DisplayName("Lễ tân xem được báo cáo mình đã chuyển lên, nhưng không xử lý được")
    void receptionist_canViewButNotHandleEscalatedReport() {
        IssueReport r = IssueReport.builder()
                .reportId(608L)
                .reporter(memberUser)
                .category(IssueReportCategory.MEMBER_FACILITY)
                .desk(IssueReportDesk.MANAGER)
                .status(IssueReportStatus.OPEN)
                .escalatedBy(receptionistUser)
                .build();
        when(issueReportRepository.findById(608L)).thenReturn(Optional.of(r));

        IssueReportResponseDTO seen = issueReportService.getReportById(608L, receptionistUser);
        assertEquals(608L, seen.getReportId());

        AppException ex = assertThrows(AppException.class, () -> issueReportService.handleReport(608L, receptionistUser,
                IssueReportHandleDTO.builder().status(IssueReportStatus.IN_PROGRESS).build()));
        assertEquals(ErrorCode.REPORT_DESK_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Lễ tân chuyển lên, Quản lý xử lý: giữ cả người chuyển lẫn người xử lý")
    void escalateThenManagerResolves_keepsBothPeople() {
        IssueReport r = IssueReport.builder()
                .reportId(604L)
                .reporter(memberUser)
                .desk(IssueReportDesk.RECEPTION)
                .status(IssueReportStatus.OPEN)
                .title("Máy chạy bộ hỏng")
                .build();
        when(issueReportRepository.findById(604L)).thenReturn(Optional.of(r));
        when(issueReportRepository.save(any(IssueReport.class))).thenAnswer(inv -> inv.getArgument(0));

        issueReportService.escalateReport(604L, receptionistUser, IssueReportEscalateDTO.builder().reason("Cần thay máy mới").build());
        IssueReportResponseDTO res = issueReportService.handleReport(604L, managerUser,
                IssueReportHandleDTO.builder().status(IssueReportStatus.RESOLVED).response("Đã đặt máy mới").build());

        assertEquals(receptionistUser.getUserId(), res.getEscalatedById());
        assertEquals(managerUser.getUserId(), res.getHandledById());
        assertEquals(IssueReportStatus.RESOLVED, res.getStatus());
    }

    @Test
    @DisplayName("Xử lý báo cáo thất bại - Lễ tân cố tình xử lý báo cáo ở bàn MANAGER")
    void handleReport_deskAccessDenied_receptionist() {
        IssueReport r = IssueReport.builder()
                .reportId(601L)
                .reporter(coachUser)
                .desk(IssueReportDesk.MANAGER)
                .status(IssueReportStatus.OPEN)
                .build();

        IssueReportHandleDTO dto = IssueReportHandleDTO.builder()
                .status(IssueReportStatus.IN_PROGRESS)
                .build();

        when(issueReportRepository.findById(601L)).thenReturn(Optional.of(r));

        AppException ex = assertThrows(AppException.class, () -> issueReportService.handleReport(601L, receptionistUser, dto));
        assertEquals(ErrorCode.REPORT_DESK_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Xử lý báo cáo thất bại - Báo cáo đã đóng (RESOLVED/REJECTED/CANCELLED)")
    void handleReport_alreadyClosed() {
        IssueReport r = IssueReport.builder()
                .reportId(602L)
                .reporter(memberUser)
                .desk(IssueReportDesk.RECEPTION)
                .status(IssueReportStatus.RESOLVED)
                .build();

        IssueReportHandleDTO dto = IssueReportHandleDTO.builder()
                .status(IssueReportStatus.IN_PROGRESS)
                .build();

        when(issueReportRepository.findById(602L)).thenReturn(Optional.of(r));

        AppException ex = assertThrows(AppException.class, () -> issueReportService.handleReport(602L, receptionistUser, dto));
        assertEquals(ErrorCode.REPORT_ALREADY_CLOSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Chuyển tiếp báo cáo lên Manager thành công (Escalate)")
    void escalateReport_success() {
        IssueReport r = IssueReport.builder()
                .reportId(700L)
                .reporter(memberUser)
                .desk(IssueReportDesk.RECEPTION)
                .status(IssueReportStatus.IN_PROGRESS)
                .title("Yêu cầu bồi hoàn gói tập")
                .build();

        IssueReportEscalateDTO dto = IssueReportEscalateDTO.builder()
                .reason("Cần phê duyệt từ cấp Quản lý")
                .build();

        when(issueReportRepository.findById(700L)).thenReturn(Optional.of(r));
        when(issueReportRepository.save(any(IssueReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByRole_RoleNameAndStatus("CENTER_MANAGER", UserStatus.ACTIVE))
                .thenReturn(List.of(managerUser));

        IssueReportResponseDTO res = issueReportService.escalateReport(700L, receptionistUser, dto);

        assertEquals(IssueReportDesk.MANAGER, res.getDesk());
        // lý do chuyển là ghi chú nội bộ, không lọt vào phản hồi gửi người báo cáo
        assertEquals("Cần phê duyệt từ cấp Quản lý", res.getEscalateReason());
        assertNull(res.getResponse());
        // chuyển cấp không phải là xử lý: lưu người chuyển riêng, chưa có người xử lý
        assertEquals(receptionistUser.getUserId(), res.getEscalatedById());
        assertNotNull(res.getEscalatedAt());
        assertNull(res.getHandledById());
        verify(notificationService).notifyUsers(eq(List.of(managerUser)), anyString(), anyString(), eq("ISSUE_REPORT_ESCALATED"), eq(receptionistUser));
        // chuyển cấp là việc nội bộ: không báo cho hội viên
        verify(notificationService, never()).notifyUsers(eq(List.of(memberUser)), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Chuyển tiếp báo cáo thất bại - Báo cáo vốn dĩ đã ở bàn MANAGER")
    void escalateReport_alreadyManagerDesk() {
        IssueReport r = IssueReport.builder()
                .reportId(701L)
                .reporter(coachUser)
                .desk(IssueReportDesk.MANAGER)
                .status(IssueReportStatus.OPEN)
                .build();

        when(issueReportRepository.findById(701L)).thenReturn(Optional.of(r));

        AppException ex = assertThrows(AppException.class, () -> issueReportService.escalateReport(701L, receptionistUser, null));
        assertEquals(ErrorCode.REPORT_CANNOT_ESCALATE, ex.getErrorCode());
    }

    @Test
    @DisplayName("Chuyển tiếp báo cáo thất bại - Báo cáo đã đóng")
    void escalateReport_alreadyClosed() {
        IssueReport r = IssueReport.builder()
                .reportId(702L)
                .reporter(memberUser)
                .desk(IssueReportDesk.RECEPTION)
                .status(IssueReportStatus.REJECTED)
                .build();

        when(issueReportRepository.findById(702L)).thenReturn(Optional.of(r));

        AppException ex = assertThrows(AppException.class, () -> issueReportService.escalateReport(702L, receptionistUser, null));
        assertEquals(ErrorCode.REPORT_ALREADY_CLOSED, ex.getErrorCode());
    }
}
