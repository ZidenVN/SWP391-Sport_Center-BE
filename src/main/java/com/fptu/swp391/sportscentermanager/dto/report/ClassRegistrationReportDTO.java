package com.fptu.swp391.sportscentermanager.dto.report;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ClassRegistrationReportDTO {
    private long totalOpeningClasses;
    private long totalRegistration;
    private long totalCancellation;
    private double averageOccupancyRate;
    private List<ClassOccurpancyDetailDTO> classDetails;
}

