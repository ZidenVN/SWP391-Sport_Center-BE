package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.report.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public interface ReportService {
  DashboardOverviewDTO getDashboardOverview();
  MemberReportDTO getMemberReport(LocalDate fromDate, LocalDate toDate);
  ClassRegistrationReportDTO getClassRegistrationReport(LocalDate fromDate, LocalDate toDate);
  RevenueReportDTO getRevenueReport(LocalDate fromDate, LocalDate toDate);
}
