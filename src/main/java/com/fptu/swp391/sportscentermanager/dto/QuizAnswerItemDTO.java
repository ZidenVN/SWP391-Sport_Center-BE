package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuizAnswerItemDTO {
    @NotNull
    private Long questionId;

    private List<Long> optionIds;

    private Double numberValue;

    @Size(max = 1000)
    private String textValue;
}
