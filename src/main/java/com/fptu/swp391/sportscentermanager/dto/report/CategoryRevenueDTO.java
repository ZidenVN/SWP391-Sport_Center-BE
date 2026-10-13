package com.fptu.swp391.sportscentermanager.dto.report;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CategoryRevenueDTO {
    private String categoryName;
    private BigDecimal totalAmount;
    private long transactionCnt;
    private double percentage;
}
