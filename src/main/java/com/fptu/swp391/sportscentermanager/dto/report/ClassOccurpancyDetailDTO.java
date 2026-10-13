package com.fptu.swp391.sportscentermanager.dto.report;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ClassOccurpancyDetailDTO{
    private Long classId;
    private String subjectName;
    private String coachName;
    private String roomName;
    private int maxCapacity;
    private long registeredCnt;
    private int availableSlots;
    private double occupancyRate;
    private String status;
}
