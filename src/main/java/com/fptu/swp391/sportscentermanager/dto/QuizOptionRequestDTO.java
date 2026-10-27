package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizOptionRequestDTO {
    private Long optionId;

    @NotBlank
    @Size(max = 255)
    private String content;

    @Size(max = 50)
    private String code;

}
