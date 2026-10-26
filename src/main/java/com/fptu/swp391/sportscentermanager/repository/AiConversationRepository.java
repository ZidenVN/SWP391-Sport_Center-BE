package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation,Long> {
    List<AiConversation> findByUser_UserIdOrderByUpdatedAtDesc(Long userId);

    // Chỉ trả về khi cuộc trò chuyện thuộc đúng user, id của người khác -> rỗng
    Optional<AiConversation> findByConversationIdAndUser_UserId(Long conversationId, Long userId);
}
