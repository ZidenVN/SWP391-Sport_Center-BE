package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassSession;
import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.entity.Room;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.enums.SessionStatus;
import com.fptu.swp391.sportscentermanager.enums.SessionType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Mục 22: không cho bỏ môn của HLV khi HLV còn buổi sắp dạy thuộc môn đó, kể cả buổi dạy thay
class CoachRepositorySubjectInUseTest extends MySqlRepositoryTest {

    @Autowired
    private CoachRepository coachRepository;
    @Autowired
    private EntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 12, 0);

    private Subject boxing;
    private Subject gym;
    private Room room;
    private Coach main;   // HLV chính của lớp Boxing
    private Coach sub;    // HLV dạy thay
    private SportClass boxingClass;
    private int seq;

    @BeforeEach
    void seed() {
        boxing = subject("Boxing");
        gym = subject("Gym");
        room = Room.builder().roomName("Phòng 1").capacity(20).status("ACTIVE").build();
        em.persist(room);
        main = coach("main");
        sub = coach("sub");
        boxingClass = SportClass.builder()
            .subject(boxing).coach(main).room(room).status("OPENING").maxCapacity(20)
            .startTime(NOW.minusDays(7)).endTime(NOW.plusDays(30))
            .build();
        em.persist(boxingClass);
    }

    private Subject subject(String name) {
        Subject s = Subject.builder().subjectName(name).build();
        em.persist(s);
        return s;
    }

    private Coach coach(String name) {
        seq++;
        Coach c = Coach.builder()
            .firstName(name).lastName("Coach").gender("Nam")
            .email(name + "@test.com").phone("09000000" + seq).passwordHash("x")
            .createdAt(LocalDateTime.now())
            .build();
        em.persist(c);
        return c;
    }

    private void session(SportClass cls, Coach teacher, LocalDateTime start, SessionStatus status) {
        em.persist(ClassSession.builder()
            .sportClass(cls).coach(teacher).room(room)
            .startTime(start).endTime(start.plusMinutes(90))
            .type(SessionType.REGULAR).status(status)
            .build());
        em.flush();
    }

    private boolean inUse(Coach coach, Subject... subjects) {
        return coachRepository.hasOpeningClassOfSubjects(
            coach.getUserId(), List.of(subjects).stream().map(Subject::getSubjectId).toList(), NOW);
    }

    @Test
    @DisplayName("HLV có buổi dạy thay Boxing sắp tới: bỏ môn Boxing bị chặn")
    void substituteUpcomingSession_blocksRemoval() {
        session(boxingClass, sub, NOW.plusDays(3), SessionStatus.SCHEDULED);

        assertTrue(inUse(sub, boxing));
    }

    @Test
    @DisplayName("HLV chính vẫn còn buổi tự dạy: bỏ môn bị chặn")
    void mainCoachUpcomingSession_blocksRemoval() {
        session(boxingClass, main, NOW.plusDays(1), SessionStatus.SCHEDULED);
        session(boxingClass, sub, NOW.plusDays(3), SessionStatus.SCHEDULED);

        assertTrue(inUse(main, boxing));
    }

    @Test
    @DisplayName("Buổi dạy thay đã qua: không chặn")
    void substitutePastSession_allowsRemoval() {
        session(boxingClass, sub, NOW.minusDays(1), SessionStatus.DONE);

        assertFalse(inUse(sub, boxing));
    }

    @Test
    @DisplayName("Buổi dạy thay sắp tới nhưng đã huỷ: không chặn")
    void substituteCancelledSession_allowsRemoval() {
        session(boxingClass, sub, NOW.plusDays(3), SessionStatus.CANCELLED);

        assertFalse(inUse(sub, boxing));
    }

    @Test
    @DisplayName("Buổi dạy thay thuộc môn khác môn bị bỏ: không chặn")
    void substituteOtherSubject_allowsRemoval() {
        session(boxingClass, sub, NOW.plusDays(3), SessionStatus.SCHEDULED);

        assertFalse(inUse(sub, gym));
        assertTrue(inUse(sub, gym, boxing), "bỏ nhiều môn cùng lúc thì chỉ cần 1 môn đang dùng là chặn");
    }

    @Test
    @DisplayName("HLV chính đã giao hết buổi sắp tới cho người khác: được bỏ môn")
    void mainCoachAllSessionsReassigned_allowsRemoval() {
        session(boxingClass, main, NOW.minusDays(2), SessionStatus.DONE);
        session(boxingClass, sub, NOW.plusDays(3), SessionStatus.SCHEDULED);

        assertFalse(inUse(main, boxing));
    }
}
