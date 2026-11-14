package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.SportClassRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SportClassResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ClassStatus;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.SessionStatus;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.ImageStorageService;
import com.fptu.swp391.sportscentermanager.service.MembershipPackageService;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SportClassServiceImplTest {

    @Mock
    private SportClassRepository sportClassRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private CoachRepository coachRepository;
    @Mock
    private ClassRegistrationRepository classRegistrationRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private ClassSessionRepository classSessionRepository;
    @Mock
    private MembershipPackageService membershipPackageService;

    @InjectMocks
    private SportClassServiceImpl sportClassService;

    private Room testRoom;
    private Subject testSubject;
    private Coach testCoach;
    private SportClass testClass;

    @BeforeEach
    void setUp() {
        testRoom = Room.builder()
                .roomId(10L)
                .roomName("Phòng Đa Năng 01")
                .capacity(30)
                .status("AVAILABLE")
                .build();

        testSubject = Subject.builder()
                .subjectId(1L)
                .subjectName("Yoga")
                .status("ACTIVE")
                .build();

        testCoach = Coach.builder()
                .speciality("Yoga Master")
                .status(UserStatus.ACTIVE)
                .build();
        testCoach.setUserId(5L);
        testCoach.setFirstName("Thành");
        testCoach.setLastName("Lê");

        testCoach.getSubjects().add(testSubject);

        testClass = SportClass.builder()
                .classId(100L)
                .room(testRoom)
                .subject(testSubject)
                .coach(testCoach)
                .maxCapacity(20)
                .status(ClassStatus.OPENING.name())
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(32))
                .durationDays(30)
                .daysOfWeek("MON,WED,FRI")
                .startClock(LocalTime.of(8, 0))
                .endClock(LocalTime.of(9, 30))
                .firstSessionAt(LocalDate.now().plusDays(2).atTime(8, 0))
                .endTime(LocalDate.now().plusDays(32).atTime(9, 30))
                .build();
    }

    @Test
    @DisplayName("Tạo lớp học theo lịch lặp cố định thành công và sinh danh sách buổi học")
    void testCreateClass_Recurring_Success() {
        LocalDate startDate = LocalDate.now(); // mở lớp ngay hôm nay được phép
        SportClassRequestDTO req = SportClassRequestDTO.builder()
                .roomId(10L)
                .subjectId(1L)
                .coachId(5L)
                .maxCapacity(25)
                .startDate(startDate)
                .durationDays(14)
                .daysOfWeek("MON,WED,FRI")
                .startClock(LocalTime.of(8, 0))
                .endClock(LocalTime.of(9, 30))
                .build();

        when(roomRepository.findByRoomIdAndStatusNot(10L, "INACTIVE")).thenReturn(Optional.of(testRoom));
        when(subjectRepository.findBySubjectIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(testSubject));
        when(coachRepository.findById(5L)).thenReturn(Optional.of(testCoach));
        when(coachRepository.findById(5L)).thenReturn(Optional.of(testCoach));
        when(membershipPackageService.getClassDurations(1L)).thenReturn(List.of(14, 30));
        when(imageStorageService.uploadClassImage(any())).thenReturn("http://img.png");
        when(classSessionRepository.existsRoomConflict(anyLong(), any(), any(), any())).thenReturn(false);
        when(classSessionRepository.existsCoachConflict(anyLong(), any(), any(), any())).thenReturn(false);
        when(sportClassRepository.save(any(SportClass.class))).thenAnswer(i -> {
            SportClass sc = i.getArgument(0);
            sc.setClassId(100L);
            return sc;
        });

        SportClassResponseDTO res = sportClassService.createClass(req, null);

        assertNotNull(res);
        assertEquals(100L, res.getClassId());
        assertEquals("Yoga", res.getSubjectName());
        assertNotNull(res.getScheduleSummary());
        assertTrue(res.getScheduleSummary().contains("08:00 - 09:30"));
        verify(classSessionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Trùng lịch ở 1 buổi giữa khoá thì không tạo gì: không lưu lớp, không lưu buổi, không upload ảnh")
    void testCreateClass_OneSessionConflict_CreatesNothing() {
        SportClassRequestDTO req = SportClassRequestDTO.builder()
                .roomId(10L).subjectId(1L).coachId(5L).maxCapacity(20)
                .startDate(LocalDate.now().plusDays(1)).durationDays(14).daysOfWeek("MON,WED,FRI")
                .startClock(LocalTime.of(8, 0)).endClock(LocalTime.of(9, 30))
                .build();

        when(roomRepository.findByRoomIdAndStatusNot(10L, "INACTIVE")).thenReturn(Optional.of(testRoom));
        when(subjectRepository.findBySubjectIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(testSubject));
        when(coachRepository.findById(5L)).thenReturn(Optional.of(testCoach));
        when(membershipPackageService.getClassDurations(1L)).thenReturn(List.of(14, 30));
        // buổi đầu trống phòng, buổi thứ 2 trùng phòng
        when(classSessionRepository.existsRoomConflict(anyLong(), any(), any(), any())).thenReturn(false, true);
        when(classSessionRepository.existsCoachConflict(anyLong(), any(), any(), any())).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> sportClassService.createClass(req, null));

        assertEquals(ErrorCode.SESSION_SCHEDULE_CONFLICT, ex.getErrorCode());
        // B6: báo rõ buổi bị trùng (buổi thứ 2 của khoá), giờ học và trùng phòng nào
        LocalDate secondSession = req.getStartDate().datesUntil(req.getStartDate().plusDays(14))
                .filter(d -> Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY).contains(d.getDayOfWeek()))
                .skip(1).findFirst().orElseThrow();
        assertTrue(ex.getMessage().contains(secondSession.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))), ex.getMessage());
        assertTrue(ex.getMessage().contains("08:00 - 09:30"), ex.getMessage());
        assertTrue(ex.getMessage().contains("trùng lịch phòng " + testRoom.getRoomName()), ex.getMessage());
        verify(sportClassRepository, never()).save(any());
        verify(classSessionRepository, never()).saveAll(anyList());
        verifyNoInteractions(imageStorageService);
    }

    @Test
    @DisplayName("Trùng lịch HLV: thông báo nói rõ trùng HLV nào, không phải trùng phòng")
    void testCreateClass_CoachConflict_NamesCoach() {
        SportClassRequestDTO req = SportClassRequestDTO.builder()
                .roomId(10L).subjectId(1L).coachId(5L).maxCapacity(20)
                .startDate(LocalDate.now().plusDays(1)).durationDays(14).daysOfWeek("MON,WED,FRI")
                .startClock(LocalTime.of(8, 0)).endClock(LocalTime.of(9, 30))
                .build();

        when(roomRepository.findByRoomIdAndStatusNot(10L, "INACTIVE")).thenReturn(Optional.of(testRoom));
        when(subjectRepository.findBySubjectIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(testSubject));
        when(coachRepository.findById(5L)).thenReturn(Optional.of(testCoach));
        when(membershipPackageService.getClassDurations(1L)).thenReturn(List.of(14, 30));
        when(classSessionRepository.existsRoomConflict(anyLong(), any(), any(), any())).thenReturn(false);
        when(classSessionRepository.existsCoachConflict(anyLong(), any(), any(), any())).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> sportClassService.createClass(req, null));

        assertEquals(ErrorCode.SESSION_SCHEDULE_CONFLICT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("trùng lịch HLV " + testCoach.getFirstName() + " " + testCoach.getLastName()),
                ex.getMessage());
        assertFalse(ex.getMessage().contains("phòng"), ex.getMessage());
    }

    @Test
    @DisplayName("Không gửi lịch lặp cố định (chỉ startTime/endTime) thì không tạo lớp đơn lẻ")
    void testCreateClass_WithoutRecurringSchedule_Rejected() {
        SportClassRequestDTO req = SportClassRequestDTO.builder()
                .roomId(10L).subjectId(1L).coachId(5L).maxCapacity(20)
                .startTime(LocalDate.now().plusDays(1).atTime(8, 0))
                .endTime(LocalDate.now().plusDays(1).atTime(9, 30))
                .build();

        when(roomRepository.findByRoomIdAndStatusNot(10L, "INACTIVE")).thenReturn(Optional.of(testRoom));
        when(subjectRepository.findBySubjectIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(testSubject));
        when(coachRepository.findById(5L)).thenReturn(Optional.of(testCoach));

        AppException ex = assertThrows(AppException.class, () -> sportClassService.createClass(req, null));

        assertEquals(ErrorCode.CLASS_DURATION_INVALID, ex.getErrorCode());
        verify(sportClassRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dừng lớp học khẩn cấp thành công và chuyển trạng thái STOPPED")
    void testStopClass_Success() {
        testClass.setFirstSessionAt(LocalDateTime.now().minusHours(2)); // Lớp đã bắt đầu
        when(sportClassRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(testClass));
        when(classRegistrationRepository.findActiveByClassId(100L)).thenReturn(Collections.emptyList());
        when(classSessionRepository.findBySportClass_ClassIdAndStartTimeAfterOrderByStartTimeAsc(eq(100L), any()))
                .thenReturn(Collections.emptyList());
        when(sportClassRepository.save(any(SportClass.class))).thenAnswer(i -> i.getArgument(0));

        SportClassResponseDTO res = sportClassService.stopClass(100L, "Hỏng hệ thống điện trung tâm");

        assertNotNull(res);
        assertEquals("STOPPED", res.getStatus());
        verify(sportClassRepository).save(testClass);
    }

    @Test
    @DisplayName("Dừng lớp thất bại khi lớp chưa bắt đầu (phải dùng chức năng Huỷ lớp)")
    void testStopClass_CannotStopBeforeStarted() {
        testClass.setFirstSessionAt(LocalDateTime.now().plusDays(2)); // Chưa bắt đầu
        when(sportClassRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(testClass));

        AppException ex = assertThrows(AppException.class,
                () -> sportClassService.stopClass(100L, "Lý do"));
        assertEquals(ErrorCode.CLASS_STOP_NOT_STARTED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Huỷ lớp thất bại khi lớp đã bắt đầu (phải dùng chức năng Dừng lớp)")
    void testCancelClass_CannotCancelAfterStarted() {
        testClass.setFirstSessionAt(LocalDateTime.now().minusHours(1)); // Đã bắt đầu
        when(sportClassRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(testClass));

        AppException ex = assertThrows(AppException.class,
                () -> sportClassService.cancelClass(100L, "Lý do huỷ"));
        assertEquals(ErrorCode.CLASS_CANCEL_STARTED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Tạo lớp thất bại khi thời hạn lớp không có trong gói đang bán")
    void testCreateClass_DurationNotSold() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        SportClassRequestDTO req = SportClassRequestDTO.builder()
                .roomId(10L)
                .subjectId(1L)
                .coachId(5L)
                .maxCapacity(25)
                .startDate(startDate)
                .durationDays(45) // Không có trong gói 14, 30
                .daysOfWeek("MON,WED,FRI")
                .startClock(LocalTime.of(8, 0))
                .endClock(LocalTime.of(9, 30))
                .build();

        when(roomRepository.findByRoomIdAndStatusNot(10L, "INACTIVE")).thenReturn(Optional.of(testRoom));
        when(subjectRepository.findBySubjectIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(testSubject));
        when(coachRepository.findById(5L)).thenReturn(Optional.of(testCoach));
        when(coachRepository.findById(5L)).thenReturn(Optional.of(testCoach));
        when(membershipPackageService.getClassDurations(1L)).thenReturn(List.of(14, 30));

        AppException ex = assertThrows(AppException.class,
                () -> sportClassService.createClass(req, null));
        assertEquals(ErrorCode.CLASS_DURATION_NOT_SOLD, ex.getErrorCode());
    }

    @Test
    @DisplayName("Tạo lớp thất bại khi ngày bắt đầu trong quá khứ")
    void testCreateClass_StartDateInPast() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        SportClassRequestDTO req = SportClassRequestDTO.builder()
                .roomId(10L)
                .subjectId(1L)
                .coachId(5L)
                .maxCapacity(25)
                .startDate(yesterday) // Hôm qua -> Không hợp lệ (hôm nay vẫn được)
                .durationDays(30)
                .daysOfWeek("MON,WED,FRI")
                .startClock(LocalTime.of(8, 0))
                .endClock(LocalTime.of(9, 30))
                .build();

        when(roomRepository.findByRoomIdAndStatusNot(10L, "INACTIVE")).thenReturn(Optional.of(testRoom));
        when(subjectRepository.findBySubjectIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(testSubject));
        when(coachRepository.findById(5L)).thenReturn(Optional.of(testCoach));

        AppException ex = assertThrows(AppException.class,
                () -> sportClassService.createClass(req, null));
        assertEquals(ErrorCode.CLASS_START_DATE_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("Cập nhật lớp Case 2: Đã có người đăng ký thì không được sửa lịch học")
    void testUpdateClass_Case2_CannotModifySchedule() {
        when(sportClassRepository.findById(100L)).thenReturn(Optional.of(testClass));
        when(classRegistrationRepository.countBySportClass_ClassIdAndStatus(100L, "REGISTERED")).thenReturn(5L);

        // Cố tình thay đổi startClock
        SportClassRequestDTO req = SportClassRequestDTO.builder()
                .roomId(10L)
                .subjectId(1L)
                .coachId(5L)
                .maxCapacity(25)
                .startDate(testClass.getStartDate())
                .durationDays(testClass.getDurationDays())
                .daysOfWeek(testClass.getDaysOfWeek())
                .startClock(LocalTime.of(10, 0)) // Đổi từ 8:00 -> 10:00
                .endClock(LocalTime.of(11, 30))
                .build();

        AppException ex = assertThrows(AppException.class,
                () -> sportClassService.updateClass(100L, req, null));
        assertEquals(ErrorCode.CLASS_SCHEDULE_CANNOT_MODIFY, ex.getErrorCode());
    }

    @Test
    @DisplayName("Cập nhật lớp Case 3: Lớp đã bắt đầu chỉ cho phép sửa sĩ số và ảnh")
    void testUpdateClass_Case3_CannotModifyCoachOrRoom() {
        // Giả lập lớp đã bắt đầu từ hôm qua
        testClass.setFirstSessionAt(LocalDateTime.now().minusDays(1));

        when(sportClassRepository.findById(100L)).thenReturn(Optional.of(testClass));
        when(classRegistrationRepository.countBySportClass_ClassIdAndStatus(100L, "REGISTERED")).thenReturn(3L);

        // Cố tình đổi sang HLV khác
        SportClassRequestDTO req = SportClassRequestDTO.builder()
                .roomId(10L)
                .subjectId(1L)
                .coachId(99L) // HLV khác
                .maxCapacity(25)
                .build();

        AppException ex = assertThrows(AppException.class,
                () -> sportClassService.updateClass(100L, req, null));
        assertEquals(ErrorCode.CLASS_CANNOT_MODIFY_STARTED, ex.getErrorCode());
    }
}
