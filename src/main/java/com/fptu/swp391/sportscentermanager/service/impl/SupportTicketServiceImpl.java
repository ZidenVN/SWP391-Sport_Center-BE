package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.SupportTicketRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SupportTicketResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.Receptionist;
import com.fptu.swp391.sportscentermanager.entity.SupportTicket;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.ReceptionistRepository;
import com.fptu.swp391.sportscentermanager.repository.SupportTicketRepository;
import com.fptu.swp391.sportscentermanager.service.SupportTicketService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final MemberRepository memberRepository;
    private final ReceptionistRepository receptionistRepository;

    @Override
    @Transactional
    public SupportTicketResponseDTO create(Long receptionistId, SupportTicketRequestDTO dto) {
        Member member = memberRepository.findById(dto.getMemberId())
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        Receptionist receptionist = receptionistRepository.findById(receptionistId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
            .content(dto.getContent().trim())
            .status("OPEN")
            .createdDate(LocalDateTime.now())
            .member(member)
            .receptionist(receptionist)
            .build());
        return toResponse(ticket);
    }

    @Override
    @Transactional
    public List<SupportTicketResponseDTO> list(Long memberId) {
        List<SupportTicket> tickets;
        if (memberId == null) {
            tickets = ticketRepository.findAllByOrderByCreatedDateDesc();
        } else {
            if (!memberRepository.existsById(memberId)) {
                throw new AppException(ErrorCode.MEMBER_NOT_FOUND);
            }
            tickets = ticketRepository.findByMember_UserIdOrderByCreatedDateDesc(memberId);
        }
        return tickets.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public SupportTicketResponseDTO resolve(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));
        ticket.setStatus("RESOLVED");
        return toResponse(ticketRepository.save(ticket));
    }

    private SupportTicketResponseDTO toResponse(SupportTicket t) {
        Member m = t.getMember();
        return SupportTicketResponseDTO.builder()
            .ticketId(t.getTicketId())
            .content(t.getContent())
            .status(t.getStatus())
            .createdDate(t.getCreatedDate())
            .memberId(m != null ? m.getUserId() : null)
            .memberName(m != null ? m.getLastName() + " " + m.getFirstName() : null)
            .receptionistId(t.getReceptionist() != null ? t.getReceptionist().getUserId() : null)
            .build();
    }
}
