package com.fptu.swp391.sportscentermanager.security;

import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.entity.WorkoutResult;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.ClassRegistrationRepository;
import com.fptu.swp391.sportscentermanager.repository.SportClassRepository;
import com.fptu.swp391.sportscentermanager.repository.WorkoutResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoachAccessGuard {
    private final SportClassRepository  sportClassRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final WorkoutResultRepository workoutResultRepository;

    // Coach phải là người dạy lớp này
    public void requireOwnsClass(Long coachId, Long classId){
        SportClass sportClass = sportClassRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        if (sportClass.getCoach() == null || !sportClass.getCoach().getUserId().equals(coachId)){
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    // member phải đang học ít nhất 1 lớp của coach này
    public void requireTeachesMember(Long coachId, Long memberId){
        if(!classRegistrationRepository.existsActiveByCoachAndMember(coachId, memberId)){
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    // member phải đang đăng kí lớp này
    public void requireMemberInClass(Long memberId, Long classId){
        boolean registered = classRegistrationRepository.findByMember_UserIdAndSportClass_ClassId(memberId, classId).map(r -> "REGISTERED".equals(r.getStatus())).orElse(false);
        if (!registered) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    // workout result phải thuộc member của coach này
    public void requireCanAccessResult(Long coachId, Long resultId){
        WorkoutResult result
             = workoutResultRepository.findById(resultId).orElseThrow(() -> new AppException(ErrorCode.RESOUCE_NOT_FOUND));
        requireTeachesMember(coachId, result.getMember().getUserId());
    }


}
