package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
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


    // BE-22: lượt đăng ký phát sinh trong kỳ (tính theo registeredAt, kể cả sau đó đã hủy)
    @Query("""
            select count(r) from ClassRegistration r
            where r.registeredAt between :start and :end
        """)
    long countRegisteredBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // BE-22: lượt hủy trong kỳ (tính theo cancelledAt, không phải registeredAt)
    @Query("""
            select count(r) from ClassRegistration r
            where r.status = 'CANCELLED' and r.cancelledAt between :start and :end
        """)
    long countCancelledBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // BE-22: tỉ lệ lấp đầy của các lớp đang mở có lịch nằm trong kỳ
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
            WHERE c.status = 'OPENING' AND c.scheduleTime BETWEEN :start AND :end
            GROUP BY c.classId, sub.subjectName, coach.lastName, coach.firstName, room.roomName, c.maxCapacity
        """)
    List<Object[]> getClassOccupancyStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // BE-15: đếm chỗ đã đăng ký cho nhiều lớp 1 lần (tránh N+1 khi trả 1 trang lớp)
    @Query("""
        SELECT r.sportClass.classId, COUNT(r) FROM ClassRegistration r
        WHERE r.status = 'REGISTERED' AND r.sportClass.classId IN :classIds
        GROUP BY r.sportClass.classId
        """)
    List<Object[]> countActiveByClassIds(@Param("classIds") Collection<Long> classIds);

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
}
