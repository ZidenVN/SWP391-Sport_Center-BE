package com.fptu.swp391.sportscentermanager.listener;

import com.fptu.swp391.sportscentermanager.dto.NotificationResponseDTO;
import com.fptu.swp391.sportscentermanager.event.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPushListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        if (event == null || event.payload() == null || event.recipientEmails() == null) {
            log.warn("NotificationCreatedEvent or its payload/recipients is null, ignoring.");
            return;
        }

        NotificationResponseDTO payload = event.payload();
        List<String> emails = event.recipientEmails();

        if (emails.isEmpty()) {
            log.debug("NotificationCreatedEvent has empty recipient list.");
            return;
        }

        log.info("Pushing notification id={} ('{}') to {} recipients via WebSocket...",
                payload.getNotificationId(), payload.getTitle(), emails.size());

        for (String email : emails) {
            if (email == null || email.isBlank()) {
                continue;
            }
            try {
                // Spring STOMP maps to destination: /user/{email}/queue/notifications
                messagingTemplate.convertAndSendToUser(email.trim(), "/queue/notifications", payload);
                log.debug("Successfully pushed notification to user={}", email);
            } catch (Exception e) {
                // Lỗi chỉ ghi log theo yêu cầu tài liệu để không làm ảnh hưởng các luồng khác
                log.error("Failed to push notification via WebSocket to user={}: {}", email, e.getMessage());
            }
        }
    }
}
