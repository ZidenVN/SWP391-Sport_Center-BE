package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassAttendance;
import com.fptu.swp391.sportscentermanager.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassAttendanceRepository extends JpaRepository<ClassAttendance, Long> {
    List<ClassAttendance> findBySportClass_ClassId(Long classId);
    Optional<ClassAttendance> findBySportClass_ClassIdAndMember_UserId(Long classId, Long memberId);

    long countByMember_UserIdAndStatus(Long memberID, AttendanceStatus status);
}
