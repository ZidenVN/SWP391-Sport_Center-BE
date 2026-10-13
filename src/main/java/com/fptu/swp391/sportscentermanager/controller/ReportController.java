package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.report.ClassRegistrationReportDTO;
import com.fptu.swp391.sportscentermanager.dto.report.DashboardOverviewDTO;
import com.fptu.swp391.sportscentermanager.dto.report.MemberReportDTO;
import com.fptu.swp391.sportscentermanager.dto.report.RevenueReportDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manager/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CENTER_MANAGER') OR hasRole('ADMIN')")
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewDTO> getOverview() {
        return ResponseEntity.ok(reportService.getDashboardOverview());
    }

    @GetMapping("/members")
    public ResponseEntity<MemberReportDTO> getMembersReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
            return ResponseEntity.ok(reportService.getMemberReport(fromDate, toDate));
    }


    @GetMapping("/classes")
    public ResponseEntity<ClassRegistrationReportDTO> getClassRegistrationReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(reportService.getClassRegistrationReport(fromDate, toDate));
    }
    @GetMapping("/revenue")
    public ResponseEntity<RevenueReportDTO> getRevenueReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(reportService.getRevenueReport(fromDate, toDate));
    }



}
