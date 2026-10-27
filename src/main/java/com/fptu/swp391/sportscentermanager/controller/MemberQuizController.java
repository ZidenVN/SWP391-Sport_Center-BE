package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.QuizQuestionResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.QuizStatusResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.QuizSubmitRequestDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.MemberQuizService;
import com.fptu.swp391.sportscentermanager.service.QuizQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members/me/quiz")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEMBER')")
public class MemberQuizController {
    private final QuizQuestionService quizQuestionService;
    private final MemberQuizService memberQuizService;

    @GetMapping("/questions")
    public ResponseEntity<List<QuizQuestionResponseDTO>> getQuestions() {
        return ResponseEntity.ok(quizQuestionService.getActiveForMember());
    }

    @GetMapping("/status")
    public ResponseEntity<QuizStatusResponseDTO> getStatus(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(memberQuizService.getStatus(principal.getUser().getUserId()));
    }

    // Nộp lần đầu hoặc làm lại: ghi đè toàn bộ câu trả lời cũ
    @PutMapping
    public ResponseEntity<QuizStatusResponseDTO> submit(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @Valid @RequestBody QuizSubmitRequestDTO dto) {
        return ResponseEntity.ok(memberQuizService.submit(principal.getUser().getUserId(), dto));
    }
}
