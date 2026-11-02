package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CoachRepository extends JpaRepository<Coach, Long> {

    // Lọc HLV theo môn khi tạo lớp
    List<Coach> findDistinctBySubjects_SubjectId(Long subjectId);

    // HLV còn buổi sắp dạy thuộc các môn này không. ClassSession.coach là người dạy thật của buổi:
    // HLV chính của lớp, hoặc HLV dạy thay nếu buổi đó được đổi người
    @Query("""
        SELECT COUNT(s) > 0 FROM ClassSession s
        WHERE s.coach.userId = :coachId
          AND s.sportClass.subject.subjectId IN :subjectIds
          AND s.status = com.fptu.swp391.sportscentermanager.enums.SessionStatus.SCHEDULED
          AND s.startTime > :now
        """)
    boolean hasOpeningClassOfSubjects(@Param("coachId") Long coachId,
                                      @Param("subjectIds") Collection<Long> subjectIds,
                                      @Param("now") LocalDateTime now);
}
