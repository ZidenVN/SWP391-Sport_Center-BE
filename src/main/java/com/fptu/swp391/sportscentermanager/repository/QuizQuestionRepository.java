package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.QuizQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    @EntityGraph(attributePaths = "options")
    List<QuizQuestion> findAllByOrderByDisplayOrderAsc();

    @EntityGraph(attributePaths = "options")
    List<QuizQuestion> findByStatusOrderByDisplayOrderAsc(String status);

    Optional<QuizQuestion> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndQuestionIdNot(String code, Long questionId);
}
