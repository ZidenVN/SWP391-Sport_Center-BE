package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.QuizStatusResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.QuizSubmitRequestDTO;

public interface MemberQuizService {
    QuizStatusResponseDTO getStatus(Long memberId);
    QuizStatusResponseDTO submit(Long memberId, QuizSubmitRequestDTO dto);
}
