package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassSession;
import com.fptu.swp391.sportscentermanager.enums.ClassSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    @Query("""
        select s from ClassSession s
        where s.status = :status
        and s.startTime <= :windowEnd
        and s.endTime >= :now
        and s.sportClass.classId in (select r.sportClass.classId from ClassRegistration r
        where r.member.userId = :memberId and r.status = 'REGISTERED')
        """)
    List<ClassSession> findMemberSessionAround(@Param("memberId") Long memberId, @Param("status") ClassSessionStatus status, @Param("now") LocalDateTime now, @Param("windowEnd") LocalDateTime windowEnd);

}
