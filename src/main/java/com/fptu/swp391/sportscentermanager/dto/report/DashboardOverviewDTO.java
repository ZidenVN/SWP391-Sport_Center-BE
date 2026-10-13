package com.fptu.swp391.sportscentermanager.dto.report;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class DashboardOverviewDTO {
    private long totalActiveMembers;
    private long newMembersThisMonth;
    private long classesToday;
    private BigDecimal revenueThisMonth;
    private double revenueGrowthRate;

}
