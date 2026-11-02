package com.fptu.swp391.sportscentermanager.guard;

import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CoachSubjectGuardTest {

    private Subject subject(long id, String name) {
        return Subject.builder().subjectId(id).subjectName(name).build();
    }

    @Test
    @DisplayName("Chấp nhận khi môn nằm trong danh sách chuyên môn của HLV")
    void testRequireTeaches_SubjectInList() {
        Subject yoga = subject(1L, "Yoga");
        Coach coach = Coach.builder().subjects(new HashSet<>(Set.of(yoga, subject(2L, "Boxing")))).build();

        assertDoesNotThrow(() -> CoachSubjectGuard.requireTeaches(coach, yoga));
    }

    @Test
    @DisplayName("Ném COACH_NOT_TEACH_SUBJECT khi môn không nằm trong chuyên môn")
    void testRequireTeaches_Mismatch_ThrowsException() {
        Coach coach = Coach.builder().subjects(new HashSet<>(Set.of(subject(1L, "Yoga")))).build();

        AppException ex = assertThrows(AppException.class,
            () -> CoachSubjectGuard.requireTeaches(coach, subject(2L, "Boxing")));
        assertEquals(ErrorCode.COACH_NOT_TEACH_SUBJECT, ex.getErrorCode());
    }

    @Test
    @DisplayName("HLV 'General Fitness' cũ không còn dạy được mọi môn")
    void testRequireTeaches_LegacySpecialityIgnored() {
        Coach coach = Coach.builder().speciality("General Fitness").build();

        AppException ex = assertThrows(AppException.class,
            () -> CoachSubjectGuard.requireTeaches(coach, subject(3L, "Gym")));
        assertEquals(ErrorCode.COACH_NOT_TEACH_SUBJECT, ex.getErrorCode());
    }

    @Test
    @DisplayName("Ném COACH_NOT_FOUND khi coach là null")
    void testRequireTeaches_NullCoach() {
        AppException ex = assertThrows(AppException.class,
            () -> CoachSubjectGuard.requireTeaches(null, subject(1L, "Yoga")));
        assertEquals(ErrorCode.COACH_NOT_FOUND, ex.getErrorCode());
    }
}
