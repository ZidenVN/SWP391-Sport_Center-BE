package com.fptu.swp391.sportscentermanager.guard;

import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoachSubjectGuardTest {

    @Test
    @DisplayName("Chấp nhận khi HLV có chuyên môn khớp tên môn học")
    void testRequireTeaches_ExactMatch() {
        Coach coach = Coach.builder().speciality("Yoga").build();
        Subject subject = Subject.builder().subjectName("Yoga").build();

        assertDoesNotThrow(() -> CoachSubjectGuard.requireTeaches(coach, subject));
    }

    @Test
    @DisplayName("Chấp nhận khi chuyên môn HLV chứa môn học dạng danh sách phẩy")
    void testRequireTeaches_CommaSeparated() {
        Coach coach = Coach.builder().speciality("Boxing, Bơi lội, Yoga").build();
        Subject subject = Subject.builder().subjectName("Yoga").build();

        assertDoesNotThrow(() -> CoachSubjectGuard.requireTeaches(coach, subject));
    }

    @Test
    @DisplayName("Chấp nhận khi chuyên môn HLV là tổng quát (All / Fitness)")
    void testRequireTeaches_GeneralFitness() {
        Coach coach = Coach.builder().speciality("General Fitness").build();
        Subject subject = Subject.builder().subjectName("Gym").build();

        assertDoesNotThrow(() -> CoachSubjectGuard.requireTeaches(coach, subject));
    }

    @Test
    @DisplayName("Ném COACH_NOT_TEACH_SUBJECT khi chuyên môn không khớp môn học")
    void testRequireTeaches_Mismatch_ThrowsException() {
        Coach coach = Coach.builder().speciality("Bơi lội").build();
        Subject subject = Subject.builder().subjectName("Boxing").build();

        AppException ex = assertThrows(AppException.class,
                () -> CoachSubjectGuard.requireTeaches(coach, subject));
        assertEquals(ErrorCode.COACH_NOT_TEACH_SUBJECT, ex.getErrorCode());
    }

    @Test
    @DisplayName("Ném COACH_NOT_FOUND khi coach là null")
    void testRequireTeaches_NullCoach() {
        Subject subject = Subject.builder().subjectName("Yoga").build();
        AppException ex = assertThrows(AppException.class,
                () -> CoachSubjectGuard.requireTeaches(null, subject));
        assertEquals(ErrorCode.COACH_NOT_FOUND, ex.getErrorCode());
    }
}
