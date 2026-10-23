package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.AiChatRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.AiChatResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.AiSuggestionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.AiSuggestionResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CoachAccessGuard;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Trợ lý AI: HLV xin gợi ý bài tập cho học viên của mình, hội viên hỏi đáp về lịch tập / dịch vụ. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final CoachAccessGuard coachAccessGuard;

    @PreAuthorize("hasRole('COACH')")
    @PostMapping("/coaches/members/{memberId}/ai-suggestions")
    public ResponseEntity<AiSuggestionResponseDTO> suggest(@AuthenticationPrincipal CustomUserDetails principal,
                                                           @PathVariable Long memberId,
                                                           @Valid @RequestBody(required = false) AiSuggestionRequestDTO dto) {
        // chỉ gợi ý cho học viên đang học lớp của HLV này
        coachAccessGuard.requireTeachesMember(principal.getUser().getUserId(), memberId);
        return ResponseEntity.ok(aiService.suggestExercises(memberId, dto != null ? dto : new AiSuggestionRequestDTO()));
    }

    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/members/me/ai-chat")
    public ResponseEntity<AiChatResponseDTO> chat(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @Valid @RequestBody AiChatRequestDTO dto) {
        return ResponseEntity.ok(aiService.chat(principal.getUser().getUserId(), dto));
    }
}
