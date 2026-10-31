package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassSession;
import com.fptu.swp391.sportscentermanager.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    List<ClassSession> findBySportClass_ClassIdOrderByStartTimeAsc(Long classId);

    List<ClassSession> findBySportClass_ClassIdAndStartTimeAfterOrderByStartTimeAsc(Long classId, LocalDateTime now);

    List<ClassSession> findBySportClass_ClassIdAndStatusOrderByStartTimeAsc(Long classId, SessionStatus status);

    Optional<ClassSession> findFirstBySportClass_ClassIdAndStatusOrderByStartTimeAsc(Long classId, SessionStatus status);

    long countBySportClass_ClassId(Long classId);

    long countBySportClass_ClassIdAndStatus(Long classId, SessionStatus status);

    void deleteBySportClass_ClassId(Long classId);

    @Query("""
        SELECT COUNT(s) > 0 FROM ClassSession s
        WHERE s.room.roomId = :roomId
          AND s.status = com.fptu.swp391.sportscentermanager.enums.SessionStatus.SCHEDULED
          AND (:excludeSessionId IS NULL OR s.sessionId <> :excludeSessionId)
          AND s.startTime < :endTime AND :startTime < s.endTime
    """)
    boolean existsRoomConflict(@Param("roomId") Long roomId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime,
                               @Param("excludeSessionId") Long excludeSessionId);

    @Query("""
        SELECT COUNT(s) > 0 FROM ClassSession s
        WHERE s.coach.userId = :coachId
          AND s.status = com.fptu.swp391.sportscentermanager.enums.SessionStatus.SCHEDULED
          AND (:excludeSessionId IS NULL OR s.sessionId <> :excludeSessionId)
          AND s.startTime < :endTime AND :startTime < s.endTime
    """)
    boolean existsCoachConflict(@Param("coachId") Long coachId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                @Param("excludeSessionId") Long excludeSessionId);

    @Query("""
        SELECT COUNT(s) FROM ClassSession s
        WHERE s.status = :status
          AND s.startTime >= :start
          AND s.startTime <= :end
    """)
    long countByStatusAndStartTimeBetween(@Param("status") SessionStatus status,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    // Dành cho A: Check-in khi Membership hết hạn nhưng có buổi học trong khung [giờ học - 30p, giờ kết thúc]
    @Query("""
        SELECT s FROM ClassSession s
        JOIN ClassRegistration r ON r.sportClass.classId = s.sportClass.classId
        WHERE r.member.userId = :memberId
          AND r.status = 'REGISTERED'
          AND s.status = com.fptu.swp391.sportscentermanager.enums.SessionStatus.SCHEDULED
          AND s.startTime <= :checkInMaxTime
          AND s.endTime >= :checkInMinTime
    """)
    List<ClassSession> findMemberSessionAround(@Param("memberId") Long memberId,
                                              @Param("checkInMinTime") LocalDateTime checkInMinTime,
                                              @Param("checkInMaxTime") LocalDateTime checkInMaxTime);
}
