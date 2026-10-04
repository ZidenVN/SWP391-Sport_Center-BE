package com.fptu.swp391.sportscentermanager.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SubjectResponseDTO {
    private Long subjectId;
    private String subjectName;
    private String description;
}
