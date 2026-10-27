package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.annotation.LogAudit;
import com.fptu.swp391.sportscentermanager.dto.QuizQuestionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.QuizQuestionResponseDTO;
import com.fptu.swp391.sportscentermanager.service.QuizQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quiz-questions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MANAGE_QUIZ')")
public class QuizQuestionController {
    private final QuizQuestionService quizQuestionService;

    @GetMapping
    public ResponseEntity<List<QuizQuestionResponseDTO>> getAll() {
        return ResponseEntity.ok(quizQuestionService.getAllForManager());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizQuestionResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(quizQuestionService.getById(id));
    }

    @LogAudit(actionType = "CREATE", targetEntity = "QuizQuestion")
    @PostMapping
    @Transactional
    public ResponseEntity<QuizQuestionResponseDTO> create(@Valid @RequestBody QuizQuestionRequestDTO dto) {
        return ResponseEntity.ok(quizQuestionService.create(dto));
    }

    @LogAudit(actionType = "UPDATE", targetEntity = "QuizQuestion")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<QuizQuestionResponseDTO> update(@PathVariable Long id, @Valid @RequestBody QuizQuestionRequestDTO dto) {
        return ResponseEntity.ok(quizQuestionService.update(id, dto));
    }

    @LogAudit(actionType = "DELETE", targetEntity = "QuizQuestion")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<String> hide(@PathVariable Long id) {
        quizQuestionService.hide(id);
        return ResponseEntity.ok("Đã ẩn câu hỏi!");
    }

    @LogAudit(actionType = "UPDATE", targetEntity = "QuizQuestion")
    @PatchMapping("/{id}/restore")
    @Transactional
    public ResponseEntity<QuizQuestionResponseDTO> restore(@PathVariable Long id) {
        return ResponseEntity.ok(quizQuestionService.restore(id));
    }
}
