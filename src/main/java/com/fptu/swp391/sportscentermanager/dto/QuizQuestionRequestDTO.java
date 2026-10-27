package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuizQuestionRequestDTO {
    @NotBlank
    private String content;
    @NotNull
    private QuestionType type;

    @Size(max = 50)
    private String code;

    private Double minValue;
    private Double maxValue;

    @Size(max = 20)
    private String unit;

    @NotNull
    @Min(1)
    private Integer displayOrder;

    @Valid
    private List<QuizOptionRequestDTO> options;
}
