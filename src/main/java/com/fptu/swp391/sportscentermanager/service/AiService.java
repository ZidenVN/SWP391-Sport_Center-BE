package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.AiChatRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.AiChatResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.AiSuggestionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.AiSuggestionResponseDTO;

public interface AiService {
    // HLV: gợi ý bài tập theo mục tiêu, trình độ, lịch sử tập của học viên
    AiSuggestionResponseDTO suggestExercises(Long memberId, AiSuggestionRequestDTO request);

    // Hội viên: hỏi đáp về lịch tập, bài tập, dịch vụ của trung tâm
    AiChatResponseDTO chat(Long memberId, AiChatRequestDTO request);
}
