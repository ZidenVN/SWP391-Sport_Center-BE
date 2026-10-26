package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.AiChatResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.AiConversationResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.AiMessageResponseDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;

import java.util.List;

public interface AiChatService {
    AiChatResponseDTO chat(CustomUserDetails principal, Long conversationId, String message);

    List<AiConversationResponseDTO> getMyConversations(Long userId);

    List<AiMessageResponseDTO> getMessages(Long userId, Long conversationId);
}
