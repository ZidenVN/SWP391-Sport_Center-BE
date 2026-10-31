package com.fptu.swp391.sportscentermanager.service.support;

import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import org.springframework.stereotype.Component;

@Component
public class CoachSubjectGuard {
    public boolean teaches(Coach coach, Subject subject){
        if (coach == null || subject == null) return false;
        Long subjectId = subject.getSubjectId();
        return coach.getSubjects().stream().anyMatch(s -> s.getSubjectId().equals(subjectId));
    }

    public void requireTeaches(Coach coach, Subject subject){
        if (!teaches(coach,subject))
            throw new AppException(ErrorCode.COACH_SUBJECT_MISMATCH);
    }

}
