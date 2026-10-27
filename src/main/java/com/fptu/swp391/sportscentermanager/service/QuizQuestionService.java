package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.QuizQuestionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.QuizQuestionResponseDTO;

import java.util.List;

public interface QuizQuestionService {
    List<QuizQuestionResponseDTO> getAllForManager();

    QuizQuestionResponseDTO getById(Long id);

    QuizQuestionResponseDTO create(QuizQuestionRequestDTO dto);

    QuizQuestionResponseDTO update(Long id, QuizQuestionRequestDTO dto);

    void hide(Long id);

    QuizQuestionResponseDTO restore(Long id);

    List<QuizQuestionResponseDTO> getActiveForMember();
}
