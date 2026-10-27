package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.QuizAnswerItemDTO;
import com.fptu.swp391.sportscentermanager.dto.QuizStatusResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.QuizSubmitRequestDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.MemberQuizAnswer;
import com.fptu.swp391.sportscentermanager.entity.QuizOption;
import com.fptu.swp391.sportscentermanager.entity.QuizQuestion;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.QuestionType;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MemberQuizAnswerRepository;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.QuizQuestionRepository;
import com.fptu.swp391.sportscentermanager.service.MemberQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberQuizServiceImpl implements MemberQuizService {
    private final MemberRepository memberRepository;
    private final QuizQuestionRepository questionRepository;
    private final MemberQuizAnswerRepository answerRepository;

    @Override
    @Transactional(readOnly = true)
    public QuizStatusResponseDTO getStatus(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->  new AppException(ErrorCode.MEMBER_NOT_FOUND));
        return toStatus(member);
    }

    @Override
    @Transactional
    public QuizStatusResponseDTO submit(Long memberId, QuizSubmitRequestDTO dto) {
        // Khoá dòng member: bấm Nộp 2 lần cùng lúc thì request sau phải đợi, tránh bị nhân đôi câu trả lời
        Member member = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));

        List<QuizQuestion> questions = questionRepository.findByStatusOrderByDisplayOrderAsc("ACTIVE");
        Map<Long, QuizQuestion> questionById = questions.stream()
            .collect(Collectors.toMap(QuizQuestion::getQuestionId, q -> q));

        // Mỗi câu chỉ trả lời 1 lần, và phải là câu đang ACTIVE
        Map<Long, QuizAnswerItemDTO> itemByQuestion = new HashMap<>();
        for (QuizAnswerItemDTO item : dto.getAnswers()) {
            if (!questionById.containsKey(item.getQuestionId())
                || itemByQuestion.put(item.getQuestionId(), item) != null) {
                throw new AppException(ErrorCode.QUIZ_INVALID_ANSWER);
            }
        }
        // Mọi câu ACTIVE đều bắt buộc
        if (itemByQuestion.size() != questions.size()) {
            throw new AppException(ErrorCode.QUIZ_MISSING_ANSWER);
        }

        LocalDateTime now = LocalDateTime.now();
        List<MemberQuizAnswer> answers = new ArrayList<>();
        for (QuizQuestion question : questions) {
            answers.addAll(buildAnswers(member, question, itemByQuestion.get(question.getQuestionId()), now));
        }

        // Làm lại = xoá toàn bộ câu trả lời cũ rồi lưu bộ mới
        answerRepository.deleteByMemberId(memberId);
        answerRepository.saveAll(answers);

        member.setQuizCompletedAt(now);
        return toStatus(member);
    }

    private List<MemberQuizAnswer> buildAnswers(Member member, QuizQuestion question,
                                                QuizAnswerItemDTO item, LocalDateTime now) {
        List<MemberQuizAnswer> result = new ArrayList<>();
        switch (question.getType()) {
            case NUMBER -> {
                Double value = item.getNumberValue();
                if (value == null) throw new AppException(ErrorCode.QUIZ_MISSING_ANSWER);
                if ((question.getMinValue() != null && value < question.getMinValue())
                    || (question.getMaxValue() != null && value > question.getMaxValue())) {
                    throw new AppException(ErrorCode.QUIZ_NUMBER_OUT_OF_RANGE);
                }
                result.add(newAnswer(member, question, now).numberValue(value).build());
                applyToProfile(member, question.getCode(), value, null);
            }
            case TEXT -> {
                if (item.getTextValue() == null || item.getTextValue().isBlank()) {
                    throw new AppException(ErrorCode.QUIZ_MISSING_ANSWER);
                }
                result.add(newAnswer(member, question, now).textValue(item.getTextValue().trim()).build());
            }
            case SINGLE, MULTIPLE -> {
                List<Long> ids = item.getOptionIds() == null ? List.of() : item.getOptionIds().stream().distinct().toList();
                if (ids.isEmpty()) throw new AppException(ErrorCode.QUIZ_MISSING_ANSWER);
                if (question.getType() == QuestionType.SINGLE && ids.size() > 1) {
                    throw new AppException(ErrorCode.QUIZ_INVALID_ANSWER);
                }
                // Chỉ được chọn lựa chọn ACTIVE của chính câu này
                Map<Long, QuizOption> activeOptions = question.getOptions().stream()
                    .filter(o -> "ACTIVE".equals(o.getStatus()))
                    .collect(Collectors.toMap(QuizOption::getOptionId, o -> o));
                for (Long id : ids) {
                    QuizOption option = activeOptions.get(id);
                    if (option == null) throw new AppException(ErrorCode.QUIZ_INVALID_ANSWER);
                    result.add(newAnswer(member, question, now).option(option).build());
                    applyToProfile(member, question.getCode(), null, option);
                }
            }
        }
        return result;
    }

    private MemberQuizAnswer.MemberQuizAnswerBuilder newAnswer(Member member, QuizQuestion question, LocalDateTime now) {
        return MemberQuizAnswer.builder().member(member).question(question).answeredAt(now);
    }

    // 3 câu hệ thống ghi thẳng vào hồ sơ member
    private void applyToProfile(Member member, String code, Double number, QuizOption option) {
        if ("HEIGHT".equals(code)) member.setHeightCm(number);
        else if ("WEIGHT".equals(code)) member.setWeightKg(number);
        else if ("TRAINING_GOAL".equals(code)) member.setTrainingGoal(option.getContent());
    }

    private QuizStatusResponseDTO toStatus(Member member) {
        return QuizStatusResponseDTO.builder()
            .completed(member.getQuizCompletedAt() != null)
            .completedAt(member.getQuizCompletedAt())
            .build();
    }
}
