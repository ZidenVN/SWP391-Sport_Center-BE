package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.AiChatRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.AiChatResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.AiConversationResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.AiMessageResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiChatService aiChatService;

    // Giai doan 1: chi member. Giai doan sau mo them role khac.
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDTO> chat(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @Valid @RequestBody AiChatRequestDTO dto) {
        return ResponseEntity.ok(aiChatService.chat(principal, dto.getConversationId(), dto.getMessage().trim()));
    }

    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/conversations")
    public ResponseEntity<List<AiConversationResponseDTO>> conversations(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(aiChatService.getMyConversations(principal.getUser().getUserId()));
    }

    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<AiMessageResponseDTO>> messages(@AuthenticationPrincipal CustomUserDetails principal,
                                                               @PathVariable Long id) {
        return ResponseEntity.ok(aiChatService.getMessages(principal.getUser().getUserId(), id));
    }
}
