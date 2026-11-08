package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.IssueReport;
import com.fptu.swp391.sportscentermanager.enums.IssueReportCategory;
import com.fptu.swp391.sportscentermanager.enums.IssueReportDesk;
import com.fptu.swp391.sportscentermanager.enums.IssueReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IssueReportRepository extends JpaRepository<IssueReport, Long> {

    List<IssueReport> findByReporter_UserIdOrderByCreatedAtDesc(Long reporterId);

    // Khoá dòng khi huỷ / xử lý / chuyển cấp: 2 người bấm cùng lúc thì người sau chờ và thấy trạng thái mới, không ghi đè nhau
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM IssueReport r WHERE r.reportId = :id")
    Optional<IssueReport> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
        SELECT r FROM IssueReport r
        LEFT JOIN FETCH r.reporter rep
        LEFT JOIN FETCH r.handledBy hb
        LEFT JOIN FETCH r.escalatedBy eb
        WHERE (:desk IS NULL OR r.desk = :desk
               OR (:includeEscalated = true AND r.escalatedBy IS NOT NULL))
          AND (:status IS NULL OR r.status = :status)
          AND (:category IS NULL OR r.category = :category)
          AND (:from IS NULL OR r.createdAt >= :from)
          AND (:to IS NULL OR r.createdAt <= :to)
          AND (:keyword IS NULL
               OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(rep.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(rep.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(rep.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """,
    countQuery = """
        SELECT COUNT(r) FROM IssueReport r
        LEFT JOIN r.reporter rep
        WHERE (:desk IS NULL OR r.desk = :desk
               OR (:includeEscalated = true AND r.escalatedBy IS NOT NULL))
          AND (:status IS NULL OR r.status = :status)
          AND (:category IS NULL OR r.category = :category)
          AND (:from IS NULL OR r.createdAt >= :from)
          AND (:to IS NULL OR r.createdAt <= :to)
          AND (:keyword IS NULL
               OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(rep.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(rep.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(rep.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<IssueReport> searchReports(@Param("desk") IssueReportDesk desk,
                                    @Param("includeEscalated") boolean includeEscalated,
                                   @Param("status") IssueReportStatus status,
                                   @Param("category") IssueReportCategory category,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);

    // Cùng điều kiện với searchReports (trừ status) để số đếm khớp danh sách
    @Query("""
        SELECT r.status, COUNT(r) FROM IssueReport r
        LEFT JOIN r.reporter rep
        WHERE (:desk IS NULL OR r.desk = :desk
               OR (:includeEscalated = true AND r.escalatedBy IS NOT NULL))
          AND (:category IS NULL OR r.category = :category)
          AND (:from IS NULL OR r.createdAt >= :from)
          AND (:to IS NULL OR r.createdAt <= :to)
          AND (:keyword IS NULL
               OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(rep.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(rep.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(rep.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
        GROUP BY r.status
    """)
    List<Object[]> countByStatus(@Param("desk") IssueReportDesk desk,
                                 @Param("includeEscalated") boolean includeEscalated,
                                 @Param("category") IssueReportCategory category,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 @Param("keyword") String keyword);
}
