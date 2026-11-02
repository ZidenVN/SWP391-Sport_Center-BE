package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CoachRepository extends JpaRepository<Coach, Long> {

    // Lọc HLV theo môn khi tạo lớp
    List<Coach> findDistinctBySubjects_SubjectId(Long subjectId);

    // HLV còn lớp chưa kết thúc (OPENING) hoặc còn buổi dạy thay chưa diễn ra thuộc các môn này không
    @Query("""
        SELECT COUNT(c) > 0 FROM SportClass c
        WHERE (c.coach.userId = :coachId
               AND c.subject.subjectId IN :subjectIds
               AND c.status = 'OPENING')
           OR EXISTS (
               SELECT s FROM ClassSession s
               WHERE s.coach.userId = :coachId
                 AND s.sportClass.subject.subjectId IN :subjectIds
                 AND s.startTime > CURRENT_TIMESTAMP
                 AND s.status = 'SCHEDULED'
           )
        """)
    boolean hasOpeningClassOfSubjects(@Param("coachId") Long coachId,
                                      @Param("subjectIds") Collection<Long> subjectIds);
}
