package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.ClassSessionResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MakeupSessionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.PostponeSessionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubstituteCoachRequestDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.SessionStatus;
import com.fptu.swp391.sportscentermanager.enums.SessionType;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceImplTest {

    @Mock
    private ClassSessionRepository classSessionRepository;
    @Mock
    private SportClassRepository sportClassRepository;
    @Mock
    private CoachRepository coachRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ClassRegistrationRepository classRegistrationRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ClassSessionServiceImpl classSessionService;

    private SportClass testClass;
    private Subject testSubject;
    private Coach testCoach;
    private Coach substituteCoach;
    private Room testRoom;
    private ClassSession testSession;

    @BeforeEach
    void setUp() {
        testSubject = Subject.builder()
                .subjectId(1L)
                .subjectName("Yoga")
                .status("ACTIVE")
                .build();

        testCoach = Coach.builder()
                .speciality("Yoga")
                .status(UserStatus.ACTIVE)
                .build();
        testCoach.setUserId(10L);
        testCoach.setFirstName("Văn A");
        testCoach.setLastName("Nguyễn");

        substituteCoach = Coach.builder()
                .speciality("Yoga, Pilates")
                .status(UserStatus.ACTIVE)
                .build();
        substituteCoach.setUserId(20L);
        substituteCoach.setFirstName("Văn B");
        substituteCoach.setLastName("Trần");

        testRoom = Room.builder()
                .roomId(100L)
                .roomName("Phòng Yoga 01")
                .capacity(30)
                .status("AVAILABLE")
                .build();

        testClass = SportClass.builder()
                .classId(1L)
                .subject(testSubject)
                .coach(testCoach)
                .room(testRoom)
                .maxCapacity(20)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .build();

        testSession = ClassSession.builder()
                .sessionId(1000L)
                .sportClass(testClass)
                .coach(testCoach)
                .room(testRoom)
                .startTime(LocalDateTime.now().plusDays(2).withHour(9).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(2).withHour(10).withMinute(30))
                .type(SessionType.REGULAR)
                .status(SessionStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách buổi học theo classId thành công")
    void testGetSessionsByClass_Success() {
        when(sportClassRepository.existsById(1L)).thenReturn(true);
        when(classSessionRepository.findBySportClass_ClassIdOrderByStartTimeAsc(1L))
                .thenReturn(List.of(testSession));

        List<ClassSessionResponseDTO> result = classSessionService.getSessionsByClass(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1000L, result.get(0).getSessionId());
        assertEquals("Yoga", result.get(0).getSubjectName());
    }

    @Test
    @DisplayName("Gán HLV dạy thay thành công và thông báo")
    void testSubstituteCoach_Success() {
        SubstituteCoachRequestDTO dto = SubstituteCoachRequestDTO.builder()
                .coachId(20L)
                .note("HLV A bận đột xuất")
                .build();

        when(classSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(coachRepository.findById(20L)).thenReturn(Optional.of(substituteCoach));
        when(classSessionRepository.existsCoachConflict(eq(20L), any(), any(), eq(1000L))).thenReturn(false);
        when(sportClassRepository.existsCoachConflict(eq(20L), any(), any(), eq(1L))).thenReturn(false);
        when(classSessionRepository.save(any(ClassSession.class))).thenAnswer(i -> i.getArgument(0));
        when(classRegistrationRepository.findActiveByClassId(1L)).thenReturn(Collections.emptyList());

        ClassSessionResponseDTO res = classSessionService.substituteCoach(1000L, dto);

        assertNotNull(res);
        assertEquals(20L, res.getCoachId());
        assertTrue(testSession.getNote().contains("HLV dạy thay"));
        verify(notificationService, atLeastOnce()).notifyUsers(any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Gán HLV dạy thay thất bại khi HLV trùng lịch")
    void testSubstituteCoach_Conflict_ThrowsException() {
        SubstituteCoachRequestDTO dto = SubstituteCoachRequestDTO.builder()
                .coachId(20L)
                .build();

        when(classSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(coachRepository.findById(20L)).thenReturn(Optional.of(substituteCoach));
        when(classSessionRepository.existsCoachConflict(eq(20L), any(), any(), eq(1000L))).thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> classSessionService.substituteCoach(1000L, dto));
        assertEquals(ErrorCode.COACH_SCHEDULE_CONFLICT, ex.getErrorCode());
    }

    @Test
    @DisplayName("Hoãn buổi học thành công")
    void testPostponeSession_Success() {
        PostponeSessionRequestDTO dto = PostponeSessionRequestDTO.builder()
                .reason("Cơ sở bảo trì điện thoại")
                .build();

        when(classSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(classSessionRepository.save(any(ClassSession.class))).thenAnswer(i -> i.getArgument(0));
        when(classRegistrationRepository.findActiveByClassId(1L)).thenReturn(Collections.emptyList());

        ClassSessionResponseDTO res = classSessionService.postponeSession(1000L, dto);

        assertNotNull(res);
        assertEquals(SessionStatus.POSTPONED, res.getStatus());
        assertTrue(testSession.getNote().contains("Hoãn"));
    }

    @Test
    @DisplayName("Xếp lịch học bù thành công và tự động kéo dài ngày kết thúc khoá nếu cần")
    void testCreateMakeupSession_Success_ExtendsClassEndDate() {
        testSession.setStatus(SessionStatus.POSTPONED);

        LocalDateTime makeupStart = LocalDateTime.now().plusDays(40).withHour(14).withMinute(0);
        LocalDateTime makeupEnd = LocalDateTime.now().plusDays(40).withHour(15).withMinute(30);

        MakeupSessionRequestDTO dto = MakeupSessionRequestDTO.builder()
                .postponedSessionId(1000L)
                .startTime(makeupStart)
                .endTime(makeupEnd)
                .build();

        when(sportClassRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(classSessionRepository.existsRoomConflict(anyLong(), any(), any(), isNull())).thenReturn(false);
        when(sportClassRepository.existsRoomConflict(anyLong(), any(), any(), anyLong())).thenReturn(false);
        when(classSessionRepository.existsCoachConflict(anyLong(), any(), any(), isNull())).thenReturn(false);
        when(sportClassRepository.existsCoachConflict(anyLong(), any(), any(), anyLong())).thenReturn(false);
        when(classSessionRepository.save(any(ClassSession.class))).thenAnswer(i -> {
            ClassSession s = i.getArgument(0);
            s.setSessionId(2000L);
            return s;
        });
        when(classRegistrationRepository.findActiveByClassId(1L)).thenReturn(Collections.emptyList());

        ClassSessionResponseDTO res = classSessionService.createMakeupSession(1L, dto);

        assertNotNull(res);
        assertEquals(SessionType.MAKEUP, res.getType());
        assertEquals(SessionStatus.SCHEDULED, res.getStatus());
        assertEquals(1000L, res.getMakeupForSessionId());

        // Kiểm tra đã tự động kéo dài endDate của testClass
        assertEquals(makeupEnd.toLocalDate(), testClass.getEndDate());
        verify(sportClassRepository).save(testClass);
    }
}
