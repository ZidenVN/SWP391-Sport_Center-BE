package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class SwitchClassRequestDTO {

    @NotNull(message = "newClassId không được để trống")
    private Long newClassId;
}
