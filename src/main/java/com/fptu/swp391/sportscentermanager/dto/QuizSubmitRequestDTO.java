package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class QuizSubmitRequestDTO {
    @NotEmpty
    @Valid
    private List<QuizAnswerItemDTO> answers;
}
