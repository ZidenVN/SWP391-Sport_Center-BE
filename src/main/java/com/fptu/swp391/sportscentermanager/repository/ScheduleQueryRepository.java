package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassSession;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phần C: các câu truy vấn lịch theo buổi (lớp đăng ký được, lịch hội viên, nhắc lịch).
 * Tách riêng khỏi ClassSessionRepository / SportClassRepository (của B) để không sửa chung file.
 */
public interface ScheduleQueryRepository extends Repository<ClassSession, Long> {

    // Lớp đăng ký được cho 1 gói lớp: cùng môn, cùng thời hạn, đang mở, chưa bắt đầu
    @Query("""
        SELECT c FROM SportClass c
        LEFT JOIN FETCH c.subject
        LEFT JOIN FETCH c.coach
        LEFT JOIN FETCH c.room
        WHERE c.subject.subjectId = :subjectId
          AND c.durationDays = :durationDays
          AND c.status = 'OPENING'
          AND c.firstSessionAt > :now
        ORDER BY c.firstSessionAt ASC
        """)
    List<SportClass> findJoinableClasses(@Param("subjectId") Long subjectId,
                                         @Param("durationDays") Integer durationDays,
                                         @Param("now") LocalDateTime now);

    // Lớp mà gói này đang đăng ký (để biết gói đang có lớp hay chưa)
    @Query("""
        SELECT r.sportClass.classId FROM ClassRegistration r
        WHERE r.subscription.subscriptionId = :subscriptionId
          AND r.status = 'REGISTERED'
        """)
    List<Long> findRegisteredClassIdsBySubscription(@Param("subscriptionId") Long subscriptionId);

    // Có buổi nào của lớp classId trùng giờ với buổi của lớp khác mà hội viên đang học không.
    // ignoreClassId: lớp sắp rời đi khi đổi lớp (truyền -1 nếu không có)
    @Query("""
        SELECT COUNT(s1) > 0 FROM ClassSession s1, ClassSession s2, ClassRegistration r
        WHERE s1.sportClass.classId = :classId
          AND s1.status <> com.fptu.swp391.sportscentermanager.enums.SessionStatus.CANCELLED
          AND r.member.userId = :memberId
          AND r.status = 'REGISTERED'
          AND r.sportClass.classId <> :classId
          AND r.sportClass.classId <> :ignoreClassId
          AND s2.sportClass.classId = r.sportClass.classId
          AND s2.status <> com.fptu.swp391.sportscentermanager.enums.SessionStatus.CANCELLED
          AND s1.startTime < s2.endTime
          AND s2.startTime < s1.endTime
        """)
    boolean existsMemberSessionConflict(@Param("memberId") Long memberId,
                                        @Param("classId") Long classId,
                                        @Param("ignoreClassId") Long ignoreClassId);

    // Lịch sắp tới của hội viên: buổi của các lớp đang học (gồm buổi bù, buổi bị hoãn để hiện trạng thái)
    @Query("""
        SELECT s FROM ClassSession s
        JOIN FETCH s.sportClass c
        LEFT JOIN FETCH c.subject
        LEFT JOIN FETCH c.coach
        LEFT JOIN FETCH s.coach
        LEFT JOIN FETCH s.room
        WHERE s.endTime > :now
          AND s.status IN (com.fptu.swp391.sportscentermanager.enums.SessionStatus.SCHEDULED, com.fptu.swp391.sportscentermanager.enums.SessionStatus.POSTPONED)
          AND c.classId IN (
              SELECT r.sportClass.classId FROM ClassRegistration r
              WHERE r.member.userId = :memberId AND r.status = 'REGISTERED')
        ORDER BY s.startTime ASC
        """)
    List<ClassSession> findMemberUpcomingSessions(@Param("memberId") Long memberId,
                                                  @Param("now") LocalDateTime now);

    // Lịch đầy đủ của hội viên (trang Lịch tập): mọi buổi của các lớp đang học, cả buổi đã qua,
    // giống lịch dạy của HLV; bỏ buổi đã huỷ
    @Query("""
        SELECT s FROM ClassSession s
        JOIN FETCH s.sportClass c
        LEFT JOIN FETCH c.subject
        LEFT JOIN FETCH c.coach
        LEFT JOIN FETCH s.coach
        LEFT JOIN FETCH s.room
        WHERE s.status <> com.fptu.swp391.sportscentermanager.enums.SessionStatus.CANCELLED
          AND c.classId IN (
              SELECT r.sportClass.classId FROM ClassRegistration r
              WHERE r.member.userId = :memberId AND r.status = 'REGISTERED')
        ORDER BY s.startTime ASC
        """)
    List<ClassSession> findMemberSessions(@Param("memberId") Long memberId);

    // Nhắc lịch: buổi bắt đầu trong (from, to]
    @Query("""
        SELECT s FROM ClassSession s
        JOIN FETCH s.sportClass c
        LEFT JOIN FETCH c.subject
        LEFT JOIN FETCH s.room
        LEFT JOIN FETCH s.coach
        WHERE s.status = com.fptu.swp391.sportscentermanager.enums.SessionStatus.SCHEDULED
          AND c.status = 'OPENING'
          AND s.startTime > :from
          AND s.startTime <= :to
        """)
    List<ClassSession> findSessionsStartingBetween(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);
}
