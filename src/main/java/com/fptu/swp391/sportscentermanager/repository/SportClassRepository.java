package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.dto.SportClassResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.ScopedValue;
import java.util.Optional;

public interface SportClassRepository extends JpaRepository<SportClass, Long> {
    Optional<SportClass> findById();
}
