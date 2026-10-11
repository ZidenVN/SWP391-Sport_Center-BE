package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class ClassRegistrationRequestDTO {

    @NotNull(message = "classId không được để trống")
    private Long classId;
}
