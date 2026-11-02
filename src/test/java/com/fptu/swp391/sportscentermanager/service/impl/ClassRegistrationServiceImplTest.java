package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.PackageType;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// LENIENT: mỗi test dừng ở một bước khác nhau của luồng đổi lớp, dùng chung bộ stub trong setUp
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassRegistrationServiceImplTest {

    @Mock private ClassRegistrationRepository registrationRepository;
    @Mock private SportClassRepository sportClassRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private ScheduleQueryRepository scheduleQueryRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ClassRegistrationServiceImpl service;

    private static final Long MEMBER_ID = 5L;
    private static final Long RECEPTIONIST_ID = 9L;
    private static final Long SUB_ID = 10L;
    private static final Long REG_ID = 50L;
    private static final Long OLD_CLASS_ID = 100L;
    private static final Long NEW_CLASS_ID = 200L;

    private Member member;
    private Subject yoga;
    private Subscription sub;
    private SportClass oldClass;
    private SportClass newClass;
    private ClassRegistration current;

    @BeforeEach
    void setUp() {
        member = Member.builder().userId(MEMBER_ID).firstName("An").lastName("Nguyễn").build();
        yoga = Subject.builder().subjectId(1L).subjectName("Yoga").status("ACTIVE").build();
        MembershipPackage pkg = MembershipPackage.builder()
            .packageId(3L).packageName("Yoga 1 tháng").packageType(PackageType.CLASS).subject(yoga).durationDays(30).build();
        sub = Subscription.builder().subscriptionId(SUB_ID).member(member).membershipPackage(pkg).status("ACTIVE").build();
        oldClass = classOf(OLD_CLASS_ID, LocalDateTime.now().plusDays(3), 30);
        newClass = classOf(NEW_CLASS_ID, LocalDateTime.now().plusDays(5), 30);
        current = ClassRegistration.builder()
            .registrationId(REG_ID).member(member).sportClass(oldClass).subscription(sub).status("REGISTERED").build();

        when(registrationRepository.findById(REG_ID)).thenReturn(Optional.of(current));
        when(registrationRepository.findByRegistrationIdAndMember_UserId(REG_ID, MEMBER_ID)).thenReturn(Optional.of(current));
        when(userRepository.findById(RECEPTIONIST_ID)).thenReturn(Optional.of(Member.builder().userId(RECEPTIONIST_ID).build()));
        when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberRepository.findByIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(member));
        when(sportClassRepository.findByIdForUpdate(NEW_CLASS_ID)).thenReturn(Optional.of(newClass));
        when(subscriptionRepository.findByIdForUpdate(SUB_ID)).thenReturn(Optional.of(sub));
        when(registrationRepository.findByMember_UserIdAndSportClass_ClassId(eq(MEMBER_ID), anyLong())).thenReturn(Optional.empty());
        when(scheduleQueryRepository.existsMemberSessionConflict(anyLong(), anyLong(), anyLong())).thenReturn(false);
        when(registrationRepository.countBySportClass_ClassIdAndStatus(anyLong(), eq("REGISTERED"))).thenReturn(0L);
        when(registrationRepository.save(any(ClassRegistration.class))).thenAnswer(i -> i.getArgument(0));
    }

    private SportClass classOf(Long id, LocalDateTime firstSessionAt, int durationDays) {
        LocalDate start = firstSessionAt.toLocalDate();
        return SportClass.builder()
            .classId(id).status("OPENING").subject(yoga).durationDays(durationDays).maxCapacity(20)
            .firstSessionAt(firstSessionAt).startDate(start).endDate(start.plusDays(durationDays - 1))
            .build();
    }

    @Test
    @DisplayName("Lễ tân đổi lớp khi lớp chưa bắt đầu: đổi được, không tính lượt")
    void receptionistSwitch_beforeStart_noTurnCounted() {
        ClassRegistrationResponseDTO res = service.switchByReceptionist(RECEPTIONIST_ID, REG_ID, NEW_CLASS_ID);

        assertEquals(NEW_CLASS_ID, res.getClassId());
        assertEquals("SWITCHED", current.getStatus());
        assertEquals(0, sub.getReceptionistSwitchCount());
        verify(subscriptionService).activateClassPackage(sub, newClass.getEndDate());
    }

    @Test
    @DisplayName("Lễ tân đổi lớp khi lớp đã bắt đầu: tính 1 lượt")
    void receptionistSwitch_afterStart_countsOneTurn() {
        oldClass.setFirstSessionAt(LocalDateTime.now().minusDays(2));

        service.switchByReceptionist(RECEPTIONIST_ID, REG_ID, NEW_CLASS_ID);

        assertEquals(1, sub.getReceptionistSwitchCount());
        assertEquals("SWITCHED", current.getStatus());
    }

    @Test
    @DisplayName("Lễ tân đổi lần 2 sau khi lớp bắt đầu: bị chặn, đăng ký cũ giữ nguyên")
    void receptionistSwitch_secondTurn_rejected() {
        oldClass.setFirstSessionAt(LocalDateTime.now().minusDays(2));
        sub.setReceptionistSwitchCount(1);

        AppException ex = assertThrows(AppException.class,
            () -> service.switchByReceptionist(RECEPTIONIST_ID, REG_ID, NEW_CLASS_ID));

        assertEquals(ErrorCode.REGISTRATION_SWITCH_LIMIT, ex.getErrorCode());
        assertEquals("REGISTERED", current.getStatus());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lớp cũ bị dừng: lễ tân chuyển lớp không tính lượt dù lớp đã bắt đầu")
    void receptionistSwitch_stoppedClass_noTurnCounted() {
        oldClass.setFirstSessionAt(LocalDateTime.now().minusDays(10));
        oldClass.setStatus("STOPPED");
        current.setStatus("STOPPED");
        sub.setReceptionistSwitchCount(1);

        service.switchByReceptionist(RECEPTIONIST_ID, REG_ID, NEW_CLASS_ID);

        assertEquals(1, sub.getReceptionistSwitchCount());
        assertEquals("SWITCHED", current.getStatus());
    }

    @Test
    @DisplayName("Hội viên tự đổi khi buổi đầu còn dưới 12 tiếng: bị chặn")
    void selfSwitch_within12Hours_rejected() {
        oldClass.setFirstSessionAt(LocalDateTime.now().plusHours(6));

        AppException ex = assertThrows(AppException.class,
            () -> service.switchSelf(MEMBER_ID, REG_ID, NEW_CLASS_ID));

        assertEquals(ErrorCode.REGISTRATION_SELF_SWITCH_CLOSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Đăng ký lớp khác thời hạn gói: bị chặn")
    void register_durationMismatch_rejected() {
        sub.setStatus("WAITING_ACTIVATION");
        SportClass longClass = classOf(NEW_CLASS_ID, LocalDateTime.now().plusDays(5), 90);
        when(sportClassRepository.findByIdForUpdate(NEW_CLASS_ID)).thenReturn(Optional.of(longClass));

        AppException ex = assertThrows(AppException.class,
            () -> service.register(MEMBER_ID, NEW_CLASS_ID, SUB_ID));

        assertEquals(ErrorCode.REGISTRATION_DURATION_MISMATCH, ex.getErrorCode());
        verify(subscriptionService, never()).activateClassPackage(any(), any());
    }

    @Test
    @DisplayName("Đăng ký lớp hợp lệ: kích hoạt gói lớp tới ngày lớp kết thúc và kích hoạt membership đang chờ")
    void register_success_activatesPackages() {
        sub.setStatus("WAITING_ACTIVATION");

        ClassRegistrationResponseDTO res = service.register(MEMBER_ID, NEW_CLASS_ID, SUB_ID);

        assertEquals("REGISTERED", res.getStatus());
        assertEquals(NEW_CLASS_ID, res.getClassId());
        verify(subscriptionService).activateClassPackage(sub, newClass.getEndDate());
        verify(subscriptionService).activateMembershipIfWaiting(eq(MEMBER_ID), any());
    }

    @Test
    @DisplayName("Đăng ký bằng gói lớp đã dùng cho lớp khác: bị chặn")
    void register_packageNotWaiting_rejected() {
        AppException ex = assertThrows(AppException.class,
            () -> service.register(MEMBER_ID, NEW_CLASS_ID, SUB_ID));

        assertEquals(ErrorCode.REGISTRATION_PACKAGE_NOT_WAITING, ex.getErrorCode());
    }
}
