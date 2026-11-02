package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Truy vấn buổi học dành cho HLV (phần C). File riêng để không đụng ClassSessionRepository của B.
@Repository
public interface CoachScheduleRepository extends JpaRepository<ClassSession, Long> {

    // Các buổi mà HLV này là người dạy thực tế (kể cả dạy thay), trong khoảng [from, to)
    @Query("""
        SELECT s FROM ClassSession s
        JOIN FETCH s.sportClass c
        LEFT JOIN FETCH c.subject
        LEFT JOIN FETCH c.coach
        JOIN FETCH s.room
        WHERE s.coach.userId = :coachId
          AND s.startTime >= :from
          AND s.startTime < :to
        ORDER BY s.startTime ASC
        """)
    List<ClassSession> findTeachingSessions(@Param("coachId") Long coachId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    // 1 buổi kèm lớp và HLV dạy buổi đó (dùng kiểm tra quyền điểm danh)
    @Query("""
        SELECT s FROM ClassSession s
        JOIN FETCH s.sportClass
        JOIN FETCH s.coach
        WHERE s.sessionId = :sessionId
        """)
    Optional<ClassSession> findDetailById(@Param("sessionId") Long sessionId);
}
