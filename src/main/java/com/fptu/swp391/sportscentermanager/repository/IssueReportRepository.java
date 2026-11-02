package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.IssueReport;
import com.fptu.swp391.sportscentermanager.enums.IssueReportCategory;
import com.fptu.swp391.sportscentermanager.enums.IssueReportDesk;
import com.fptu.swp391.sportscentermanager.enums.IssueReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IssueReportRepository extends JpaRepository<IssueReport, Long> {

    List<IssueReport> findByReporter_UserIdOrderByCreatedAtDesc(Long reporterId);

    Page<IssueReport> findByReporter_UserIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);

    @Query(value = """
        SELECT r FROM IssueReport r
        LEFT JOIN FETCH r.reporter rep
        LEFT JOIN FETCH r.handledBy hb
        WHERE (:desk IS NULL OR r.desk = :desk)
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
        WHERE (:desk IS NULL OR r.desk = :desk)
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
                                   @Param("status") IssueReportStatus status,
                                   @Param("category") IssueReportCategory category,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);
}
