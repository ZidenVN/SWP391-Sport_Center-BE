package com.fptu.swp391.sportscentermanager.event;

import com.fptu.swp391.sportscentermanager.dto.NotificationResponseDTO;

import java.util.List;

public record NotificationCreatedEvent(NotificationResponseDTO payload, List<String> recipientEmails) {
}
