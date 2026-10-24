package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.CheckInResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface CheckInService {
    CheckInResponseDTO checkIn(Long memberId, Long receptionistId);
    List<CheckInResponseDTO> getCheckIns(LocalDate date);
}
