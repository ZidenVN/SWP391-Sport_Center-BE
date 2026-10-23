package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.SportClass;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SportClassRepository extends JpaRepository<SportClass, Long> {

    // Khóa dòng của lớp trong lúc đăng ký: 2 người bấm cùng lúc sẽ xếp hàng,
    // không thể cùng lọt vào chỗ cuối cùng. Phải gọi trong @Transactional.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM SportClass c WHERE c.classId = :id")
    Optional<SportClass> findByIdForUpdate(@Param("id") Long id);

    List<SportClass> findByCoach_UserIdOrderByStartTimeAsc(Long coachId);

    List<SportClass> findByCoach_UserIdAndStartTimeBetweenOrderByStartTimeAsc(Long coachId, LocalDateTime start, LocalDateTime end);

    // BE-15: tìm lớp theo khoảng ngày / môn / HLV / phòng / trạng thái / còn chỗ. Tham số null = bỏ qua filter đó.
    // LEFT JOIN coach để lớp chưa có HLV vẫn hiện ra khi không lọc theo HLV.
    @Query(value = """
        SELECT c FROM SportClass c
        LEFT JOIN c.coach co
        WHERE (:from IS NULL OR c.startTime >= :from)
          AND (:to IS NULL OR c.startTime <= :to)
          AND (:subjectId IS NULL OR c.subject.subjectId = :subjectId)
          AND (:roomId IS NULL OR c.room.roomId = :roomId)
          AND (:coachId IS NULL OR co.userId = :coachId)
          AND (:status IS NULL OR c.status = :status)
          AND (:available IS NULL
               OR (:available = TRUE AND c.maxCapacity > (SELECT COUNT(r) FROM ClassRegistration r
                                                          WHERE r.sportClass = c AND r.status = 'REGISTERED'))
               OR (:available = FALSE AND c.maxCapacity <= (SELECT COUNT(r) FROM ClassRegistration r
                                                            WHERE r.sportClass = c AND r.status = 'REGISTERED')))
        """,
        countQuery = """
        SELECT COUNT(c) FROM SportClass c
        LEFT JOIN c.coach co
        WHERE (:from IS NULL OR c.startTime >= :from)
          AND (:to IS NULL OR c.startTime <= :to)
          AND (:subjectId IS NULL OR c.subject.subjectId = :subjectId)
          AND (:roomId IS NULL OR c.room.roomId = :roomId)
          AND (:coachId IS NULL OR co.userId = :coachId)
          AND (:status IS NULL OR c.status = :status)
          AND (:available IS NULL
               OR (:available = TRUE AND c.maxCapacity > (SELECT COUNT(r) FROM ClassRegistration r
                                                          WHERE r.sportClass = c AND r.status = 'REGISTERED'))
               OR (:available = FALSE AND c.maxCapacity <= (SELECT COUNT(r) FROM ClassRegistration r
                                                            WHERE r.sportClass = c AND r.status = 'REGISTERED')))
        """)
    Page<SportClass> search(@Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            @Param("subjectId") Long subjectId,
                            @Param("roomId") Long roomId,
                            @Param("coachId") Long coachId,
                            @Param("status") String status,
                            @Param("available") Boolean available,
                            Pageable pageable);

    // BE-22: số lớp diễn ra trong ngày
    long countByStatusAndStartTimeBetween(String status, LocalDateTime start, LocalDateTime end);

    // BE-27: HLV còn lớp sắp dạy thì không cho khóa
    boolean existsByCoach_UserIdAndStatusAndStartTimeAfter(Long coachId, String status, LocalDateTime time);

    // Kiểm tra trùng lịch: chỉ tính lớp đang mở (lớp đã hủy / đã kết thúc không còn giữ phòng, giữ HLV)
    @Query("""
        SELECT COUNT(c) > 0 FROM SportClass c
        WHERE c.room.roomId = :roomId
        AND c.classId <> :excludeClassId
        AND c.status = 'OPENING'
        AND c.startTime < :endTime
        AND :startTime < c.endTime
        """)
    boolean existsRoomConflict(@Param("roomId") Long roomId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime,
                               @Param("excludeClassId") Long excludeClassId);

    @Query("""
        SELECT COUNT(c) > 0 FROM SportClass c
        WHERE c.coach.userId = :coachId
        AND c.classId <> :excludeClassId
        AND c.status = 'OPENING'
        AND c.startTime < :endTime
        AND :startTime < c.endTime
        """)
    boolean existsCoachConflict(@Param("coachId") Long coachId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                @Param("excludeClassId") Long excludeClassId);

    // BE-12: phòng còn lớp ĐANG MỞ cần nhiều chỗ hơn sức chứa mới (lớp đã hủy/kết thúc không tính)
    boolean existsByRoom_RoomIdAndStatusAndMaxCapacityGreaterThan(Long roomId, String status, Integer capacity);

    boolean existsByRoom_RoomIdAndStatusAndStartTimeAfter(Long roomId, String status, LocalDateTime time);
    boolean existsBySubject_SubjectIdAndStatusAndStartTimeAfter(Long subjectId, String status, LocalDateTime time);
}
