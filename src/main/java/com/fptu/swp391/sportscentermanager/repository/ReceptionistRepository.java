package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Receptionist;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReceptionistRepository extends JpaRepository<Receptionist, Long> {

    // BE-25: tìm lễ tân theo tên/email/sđt và trạng thái. Tham số null = bỏ qua filter đó.
    @Query("""
        SELECT r FROM Receptionist r
        WHERE (:status IS NULL OR r.status = :status)
          AND (:kw IS NULL
               OR LOWER(CONCAT(r.lastName, ' ', r.firstName)) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(r.email) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR r.phone LIKE CONCAT('%', :kw, '%'))
        ORDER BY r.userId DESC
        """)
    List<Receptionist> search(@Param("kw") String keyword, @Param("status") UserStatus status);
}
