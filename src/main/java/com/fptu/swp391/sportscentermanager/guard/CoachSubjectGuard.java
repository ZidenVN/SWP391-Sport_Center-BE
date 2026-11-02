package com.fptu.swp391.sportscentermanager.guard;

import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;

public class CoachSubjectGuard {

    // Phải gọi trong @Transactional vì Coach.subjects là LAZY
    public static void requireTeaches(Coach coach, Subject subject) {
        if (coach == null) {
            throw new AppException(ErrorCode.COACH_NOT_FOUND);
        }
        if (subject == null) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        boolean matches = coach.getSubjects() != null && coach.getSubjects().stream()
            .anyMatch(s -> s.getSubjectId().equals(subject.getSubjectId()));
        if (!matches) {
            throw new AppException(ErrorCode.COACH_NOT_TEACH_SUBJECT);
        }
    }
}
