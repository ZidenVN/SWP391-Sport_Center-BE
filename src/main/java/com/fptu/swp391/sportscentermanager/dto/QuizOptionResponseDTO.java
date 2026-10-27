package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

@Getter @Setter @Builder
public class QuizOptionResponseDTO {
    private Long optionId;
    private String content;
    private String code;
    private Integer displayOrder;
    private String status;
}
