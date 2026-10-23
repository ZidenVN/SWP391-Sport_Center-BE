package com.fptu.swp391.sportscentermanager.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import com.fptu.swp391.sportscentermanager.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // BE-26: lọc nhật ký theo khoảng ngày / hành động / đối tượng / người thực hiện. Tham số null = bỏ qua.
    @Query(value = """
        SELECT a FROM AuditLog a
        LEFT JOIN FETCH a.user u
        WHERE (:from IS NULL OR a.createdAt >= :from)
          AND (:to IS NULL OR a.createdAt <= :to)
          AND (:actionType IS NULL OR a.actionType = :actionType)
          AND (:targetEntity IS NULL OR a.targetEntity = :targetEntity)
          AND (:userId IS NULL OR u.userId = :userId)
        """,
        countQuery = """
        SELECT COUNT(a) FROM AuditLog a
        LEFT JOIN a.user u
        WHERE (:from IS NULL OR a.createdAt >= :from)
          AND (:to IS NULL OR a.createdAt <= :to)
          AND (:actionType IS NULL OR a.actionType = :actionType)
          AND (:targetEntity IS NULL OR a.targetEntity = :targetEntity)
          AND (:userId IS NULL OR u.userId = :userId)
        """)
    Page<AuditLog> search(@Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          @Param("actionType") String actionType,
                          @Param("targetEntity") String targetEntity,
                          @Param("userId") Long userId,
                          Pageable pageable);
}
