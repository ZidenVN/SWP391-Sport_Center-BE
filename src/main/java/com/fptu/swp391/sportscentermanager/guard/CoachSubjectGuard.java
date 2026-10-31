package com.fptu.swp391.sportscentermanager.guard;

import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;

public class CoachSubjectGuard {

    public static void requireTeaches(Coach coach, Subject subject) {
        if (coach == null) {
            throw new AppException(ErrorCode.COACH_NOT_FOUND);
        }
        if (subject == null) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }

        boolean matches = false;

        if (coach.getSpeciality() != null && !coach.getSpeciality().isBlank()) {
            String spec = coach.getSpeciality().trim().toLowerCase();
            String subName = subject.getSubjectName() != null ? subject.getSubjectName().trim().toLowerCase() : "";

            if (spec.contains(subName) || subName.contains(spec)
                    || "all".equalsIgnoreCase(spec)
                    || "general fitness".equalsIgnoreCase(spec)
                    || "fitness".equalsIgnoreCase(spec)) {
                matches = true;
            } else {
                String[] tokens = spec.split("[,;/|\\s]+");
                for (String t : tokens) {
                    if (t.length() >= 3 && subName.contains(t)) {
                        matches = true;
                        break;
                    }
                }
            }
        }

        if (!matches) {
            throw new AppException(ErrorCode.COACH_NOT_TEACH_SUBJECT);
        }
    }
}
