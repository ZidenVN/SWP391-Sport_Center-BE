package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("""
        SELECT u FROM User u
        WHERE (:keyword IS NULL
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR u.phone LIKE CONCAT('%', :keyword, '%'))
          AND (:roleName IS NULL OR u.role.roleName = :roleName)
          AND (:status IS NULL OR u.status = :status)
        """)
    Page<User> searchUsers(@Param("keyword") String keyword,
                           @Param("roleName") String roleName,
                           @Param("status") UserStatus status,
                           Pageable pageable);
}
