package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage,Long> {
    List<AiMessage> findByConversation_ConversationIdOrderByCreatedAtAsc(Long conversationId);

    long countByConversation_User_UserIdAndRoleAndCreatedAtAfter(Long userId, String role, LocalDateTime from);
}
