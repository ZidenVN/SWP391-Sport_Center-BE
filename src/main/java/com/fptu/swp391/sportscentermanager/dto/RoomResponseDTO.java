package com.fptu.swp391.sportscentermanager.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder

public class RoomResponseDTO {
    private Long roomId;
    private String roomName;
    private Integer capacity;
    private String status;
}
