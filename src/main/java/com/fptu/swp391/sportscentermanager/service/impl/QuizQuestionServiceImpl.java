package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.QuizOptionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.QuizOptionResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.QuizQuestionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.QuizQuestionResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.QuizOption;
import com.fptu.swp391.sportscentermanager.entity.QuizQuestion;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.QuestionType;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MemberQuizAnswerRepository;
import com.fptu.swp391.sportscentermanager.repository.QuizQuestionRepository;
import com.fptu.swp391.sportscentermanager.service.QuizQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizQuestionServiceImpl implements QuizQuestionService {
    private final QuizQuestionRepository questionRepository;
    private final MemberQuizAnswerRepository answerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<QuizQuestionResponseDTO> getAllForManager() {
        return questionRepository.findAllByOrderByDisplayOrderAsc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizQuestionResponseDTO getById(Long id) {
        return toResponse(findQuestion(id));
    }

    @Override
    @Transactional
    public QuizQuestionResponseDTO create(QuizQuestionRequestDTO dto) {
        validate(dto);
        String code = normalizeCode(dto.getCode());
        if (code != null && questionRepository.existsByCode(code)) {
            throw new AppException(ErrorCode.QUIZ_CODE_TAKEN);
        }
        QuizQuestion question = QuizQuestion.builder()
            .content(dto.getContent().trim())
            .type(dto.getType())
            .code(code)
            .displayOrder(dto.getDisplayOrder())
            .build();
        applyNumberConfig(question, dto);
        syncOptions(question, dto.getOptions(), false);
        return toResponse(questionRepository.save(question));
    }

    @Override
    @Transactional
    public QuizQuestionResponseDTO update(Long id, QuizQuestionRequestDTO dto) {
        QuizQuestion question = findQuestion(id);
        if (question.isSystemQuestion()){
            throw new AppException(ErrorCode.QUIZ_SYSTEM_QUESTION_LOCKED);
        }

        boolean answered = answerRepository.existsByQuestion_QuestionId(id);
        if (answered && dto.getType() != question.getType()) {
            throw new AppException(ErrorCode.QUIZ_TYPE_LOCKED);
        }

        validate(dto);
        String code = normalizeCode(dto.getCode());
        if (code != null && questionRepository.existsByCodeAndQuestionIdNot(code, id)) {
            throw new AppException(ErrorCode.QUIZ_CODE_TAKEN);
        }

        question.setContent(dto.getContent().trim());
        question.setType(dto.getType());
        question.setCode(code);
        question.setDisplayOrder(dto.getDisplayOrder());
        applyNumberConfig(question, dto);
        syncOptions(question, dto.getOptions(), true);
        // question đang được quản lý: flush để option mới có id ngay, không cần save (merge)
        questionRepository.flush();
        return toResponse(question);

    }

    @Override
    @Transactional
    public void hide(Long id) {
        QuizQuestion question = findQuestion(id);
        if(question.isSystemQuestion()){
            throw new AppException(ErrorCode.QUIZ_SYSTEM_QUESTION_LOCKED);
        }

        question.setStatus("INACTIVE");
    }

    @Override
    @Transactional
    public QuizQuestionResponseDTO restore(Long id) {
        QuizQuestion question = findQuestion(id);
        question.setStatus("ACTIVE");
        return toResponse(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizQuestionResponseDTO> getActiveForMember() {
        return questionRepository.findByStatusOrderByDisplayOrderAsc("ACTIVE").stream().map(q -> toResponse(q, true)).toList();
    }

    private void validate(QuizQuestionRequestDTO dto) {
        int optionCnt = dto.getOptions() == null ? 0 : dto.getOptions().size();
        if (hasOptions(dto.getType()) ? optionCnt < 2 : optionCnt > 0){
            throw new AppException(ErrorCode.QUIZ_INVALID_OPTIONS);
        }
        if (dto.getType() == QuestionType.NUMBER && dto.getMinValue() != null && dto.getMaxValue() != null && dto.getMinValue() > dto.getMaxValue()) {
            throw new AppException(ErrorCode.QUIZ_INVALID_RANGE);
        }
    }

    private boolean hasOptions(QuestionType type) {
        return type == QuestionType.SINGLE || type == QuestionType.MULTIPLE;
    }

    private QuizQuestion findQuestion(Long id){
        return questionRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));
    }

    private void applyNumberConfig(QuizQuestion question, QuizQuestionRequestDTO dto) {
        boolean isNumber = dto.getType() == QuestionType.NUMBER;
        question.setMinValue(isNumber ? dto.getMinValue() : null);
        question.setMaxValue(isNumber ? dto.getMaxValue() : null);
        question.setUnit(isNumber ? dto.getUnit() : null);
    }

    private String normalizeCode(String code) {
        return (code == null || code.isBlank() ? null : code.trim().toUpperCase());
    }

    private void syncOptions(QuizQuestion question, List<QuizOptionRequestDTO> requests, boolean checkAnswers) {
        List<QuizOptionRequestDTO> items = requests == null ? List.of() : requests;
        // Lấy trước các option đã có người chọn: nếu query giữa lúc đang sửa danh sách,
        // Hibernate sẽ flush sớm -> option mới có id và bị tưởng là option cũ bị bỏ
        Set<Long> answeredIds = checkAnswers
            ? answerRepository.findAnsweredOptionIds(question.getQuestionId())
            : Set.of();
        Map<Long, QuizOption> existing = question.getOptions().stream()
            .collect(Collectors.toMap(QuizOption::getOptionId, o -> o));
        Set<Long> keptIds = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {
            QuizOptionRequestDTO item = items.get(i);
            QuizOption option;
            if (item.getOptionId() != null) {
                option = existing.get(item.getOptionId());
                if (option == null) {
                    throw new AppException(ErrorCode.QUIZ_OPTION_NOT_FOUND);
                }
                keptIds.add(option.getOptionId());
            } else {
                option = QuizOption.builder().question(question).build();
                question.getOptions().add(option);
            }
            option.setContent(item.getContent().trim());
            option.setCode(normalizeCode(item.getCode()));
            option.setDisplayOrder(i + 1);
            option.setStatus("ACTIVE");
        }

        // Chỉ xét các option cũ không còn trong danh sách gửi lên
        int nextOrder = items.size() + 1;
        for (QuizOption old : existing.values()) {
            if (keptIds.contains(old.getOptionId())) {
                continue;
            }
            if (answeredIds.contains(old.getOptionId())) {
                old.setStatus("INACTIVE");
                old.setDisplayOrder(nextOrder++); // xếp sau các option đang dùng
            } else {
                question.getOptions().remove(old); // orphanRemoval sẽ xoá dòng này
            }
        }
    }
    private QuizQuestionResponseDTO toResponse(QuizQuestion q) {
        return toResponse(q, false);
    }

    // activeOptionsOnly = true: Member chỉ thấy lựa chọn đang ACTIVE
    private QuizQuestionResponseDTO toResponse(QuizQuestion q, boolean activeOptionsOnly) {
        return QuizQuestionResponseDTO.builder()
            .questionId(q.getQuestionId())
            .content(q.getContent())
            .type(q.getType())
            .code(q.getCode())
            .minValue(q.getMinValue())
            .maxValue(q.getMaxValue())
            .unit(q.getUnit())
            .displayOrder(q.getDisplayOrder())
            .status(q.getStatus())
            .systemQuestion(q.isSystemQuestion())
            .options(q.getOptions().stream()
                .filter(o -> !activeOptionsOnly || "ACTIVE".equals(o.getStatus()))
                .map(o -> QuizOptionResponseDTO.builder()
                .optionId(o.getOptionId())
                .content(o.getContent())
                .code(o.getCode())
                .displayOrder(o.getDisplayOrder())
                .status(o.getStatus())
                .build()).toList())
            .build();
    }

}
