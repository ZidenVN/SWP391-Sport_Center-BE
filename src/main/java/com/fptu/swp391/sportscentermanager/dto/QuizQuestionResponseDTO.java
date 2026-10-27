package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.QuestionType;
import lombok.*;

import java.util.List;

@Getter @Setter @Builder
public class QuizQuestionResponseDTO {
    private Long questionId;
    private String content;
    private QuestionType type;
    private String code;
    private Double minValue;
    private Double maxValue;
    private String unit;
    private Integer displayOrder;
    private String status;
    private boolean systemQuestion;
    private List<QuizOptionResponseDTO> options;
}
