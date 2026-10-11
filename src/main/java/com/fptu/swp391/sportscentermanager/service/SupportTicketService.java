package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.SupportTicketRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SupportTicketResponseDTO;

import java.util.List;

public interface SupportTicketService {

    SupportTicketResponseDTO create(Long receptionistId, SupportTicketRequestDTO dto);

    // memberId = null thì lấy tất cả
    List<SupportTicketResponseDTO> list(Long memberId);

    SupportTicketResponseDTO resolve(Long ticketId);
}
