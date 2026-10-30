package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.ReceptionistNotificationRequestDTO;

public interface ReceptionistNotificationService {
    // Trả về số member nhận được tin
    int send(Long senderId, ReceptionistNotificationRequestDTO dto);
}
