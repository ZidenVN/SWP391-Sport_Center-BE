package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClassRegistrationRepository extends JpaRepository<ClassRegistration, Long> {

    // Tìm dòng đăng ký của 1 member với 1 lớp (có thể đang REGISTERED hoặc đã CANCELLED)
    Optional<ClassRegistration> findByMember_UserIdAndSportClass_ClassId(Long memberId, Long classId);

    // Đếm số chỗ đang bị chiếm của lớp
    long countBySportClass_ClassIdAndStatus(Long classId, String status);

    // Lấy 1 đăng ký, chỉ khi nó thuộc về member này (tránh member A hủy lớp của member B)
    Optional<ClassRegistration> findByRegistrationIdAndMember_UserId(Long registrationId, Long memberId);

    // Lịch sử đăng ký: mới nhất trước.
    // JOIN FETCH để lấy luôn lớp/phòng/môn/HLV trong 1 câu SQL (tránh N+1)
    @Query("""
        SELECT r FROM ClassRegistration r
        JOIN FETCH r.sportClass c
        LEFT JOIN FETCH c.room
        LEFT JOIN FETCH c.subject
        LEFT JOIN FETCH c.coach
        WHERE r.member.userId = :memberId
        ORDER BY r.registeredAt DESC
        """)
    List<ClassRegistration> findHistory(@Param("memberId") Long memberId);

    // Lịch sắp tới: đang REGISTERED và lớp chưa diễn ra, gần nhất trước
    @Query("""
        SELECT r FROM ClassRegistration r
        JOIN FETCH r.sportClass c
        LEFT JOIN FETCH c.room
        LEFT JOIN FETCH c.subject
        LEFT JOIN FETCH c.coach
        WHERE r.member.userId = :memberId
          AND r.status = 'REGISTERED'
          AND c.startTime > :now
        ORDER BY c.startTime ASC
        """)
    List<ClassRegistration> findUpcoming(@Param("memberId") Long memberId,
                                         @Param("now") LocalDateTime now);


    @Query("""
            select count(r) from ClassRegistration r
            where r.status = :status and r.registeredAt between :start and :end
        """)
    long countByStatusBetween(@Param("status") String status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT c.classId,
                   sub.subjectName,
                   CONCAT(coach.lastName, ' ', coach.firstName),
                   room.roomName,
                   c.maxCapacity,
                   COUNT(CASE WHEN r.status = 'REGISTERED' THEN 1 END) AS registeredCount
            FROM SportClass c
            JOIN c.subject sub
            LEFT JOIN c.coach coach
            LEFT JOIN c.room room
            LEFT JOIN ClassRegistration r ON r.sportClass.classId = c.classId
            GROUP BY c.classId, sub.subjectName, coach.lastName, coach.firstName, room.roomName, c.maxCapacity
        """)
    List<Object[]> getClassOccupancyStats();

    // Danh sách học viên đang đăng ký của 1 lớp (roster + điểm danh). JOIN FETCH để tránh N+1
    @Query("""
        SELECT r FROM ClassRegistration r
        JOIN FETCH r.member
        WHERE r.sportClass.classId = :classId
          AND r.status = 'REGISTERED'
        ORDER BY r.registeredAt ASC
        """)
    List<ClassRegistration> findActiveByClassId(@Param("classId") Long classId);

    @Query("""
        SELECT COUNT(r) > 0 FROM ClassRegistration r
        WHERE r.member.userId = :memberId
          AND r.sportClass.coach.userId = :coachId
          AND r.status = 'REGISTERED'
        """)
    boolean existsActiveByCoachAndMember(@Param("coachId") Long coachId, @Param("memberId") Long memberId);

    @Query("""
        SELECT r.sportClass.classId FROM ClassRegistration r
        WHERE r.member.userId = :memberId
        AND r.status = 'REGISTERED'
        """)
    List<Long> findActiveClassIdsByMember(@Param("memberId") Long memberId);

    @Query("""
        SELECT COUNT(r) > 0 FROM ClassRegistration r
        WHERE r.member.userId = :memberId
        AND r.status = 'REGISTERED'
        AND r.sportClass.classId <> :classId
        AND r.sportClass.startTime < :endTime
        AND :startTime < r.sportClass.endTime
        """)

    boolean existsMemberScheduleConflict(@Param("memberId") Long memberId,
                                         @Param("classId") Long classId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

}
