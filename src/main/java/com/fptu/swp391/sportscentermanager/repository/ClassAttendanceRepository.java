package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassAttendance;
import com.fptu.swp391.sportscentermanager.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassAttendanceRepository extends JpaRepository<ClassAttendance, Long> {
    List<ClassAttendance> findByClassSession_SessionId(Long sessionId);

    long countByMember_UserIdAndStatus(Long memberID, AttendanceStatus status);

    @Query("""
        select a from ClassAttendance a
        join fetch a.classSession s
        join fetch s.sportClass c
        left join fetch c.subject
        left join fetch s.coach
        left join fetch s.room
        where a.member.userId = :memberId
        order by s.startTime desc
        """)
    List<ClassAttendance> findHistoryByMember(@Param("memberId") Long memberId);
}
