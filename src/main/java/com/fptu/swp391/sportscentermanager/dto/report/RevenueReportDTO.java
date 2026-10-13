package com.fptu.swp391.sportscentermanager.dto.report;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class RevenueReportDTO {
    private BigDecimal totalRevenue;
    private long totalTransactions;
    private List<TimeSeriesDataPoint> revenueOvertime;
    private List<CategoryRevenueDTO> revenueByPackage;
    private List<CategoryRevenueDTO> revenueByMethod;
}
