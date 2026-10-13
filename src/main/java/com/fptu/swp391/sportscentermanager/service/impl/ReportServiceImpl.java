package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.report.*;
import com.fptu.swp391.sportscentermanager.repository.ClassRegistrationRepository;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.PaymentRepository;
import com.fptu.swp391.sportscentermanager.repository.SportClassRepository;
import com.fptu.swp391.sportscentermanager.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final MemberRepository memberRepository;
    private final SportClassRepository sportClassRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public DashboardOverviewDTO getDashboardOverview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX);

        LocalDateTime startOfLastMonth = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime endOfLastMonth = YearMonth.now().minusMonths(1).atEndOfMonth().atTime(LocalTime.MAX);

        long activeMembers = memberRepository.countActiveSubscriptionMembers(now);
        long newMembersThisMonth = memberRepository.countNewMembersBetween(startOfMonth, endOfMonth);
        BigDecimal revThisMonth = paymentRepository.sumReveneBetween(startOfMonth, endOfMonth);
        BigDecimal revLastMonth = paymentRepository.sumReveneBetween(startOfLastMonth, endOfLastMonth);

        double growthRate = 0.0;
        if (revLastMonth != null && revLastMonth.compareTo(BigDecimal.ZERO) > 0) {
            growthRate = revThisMonth.subtract(revLastMonth)
                    .divide(revLastMonth, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return DashboardOverviewDTO.builder()
                .totalActiveMembers(activeMembers)
                .newMembersThisMonth(newMembersThisMonth)
                .revenueThisMonth(revThisMonth)
                .revenueGrowthRate(growthRate)
                .build();
    }

    @Override
    public MemberReportDTO getMemberReport(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = (fromDate != null ? fromDate : LocalDate.now().minusDays(30)).atStartOfDay();
        LocalDateTime end = (toDate != null ? toDate : LocalDate.now()).atTime(LocalTime.MAX);

        long total = memberRepository.countTotalMembers();
        long newInRange = memberRepository.countNewMembersBetween(start, end);
        long activeSub = memberRepository.countActiveSubscriptionMembers(LocalDateTime.now());
        long expiredOrNone = Math.max(0, total - activeSub);

        return MemberReportDTO.builder()
                .totalMembers(total)
                .newMembersInRange(newInRange)
                .activeSubscriptionMembers(activeSub)
                .expiredOrNoSubMembers(expiredOrNone)
                .build();
    }

    @Override
    public ClassRegistrationReportDTO getClassRegistrationReport(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = (fromDate != null ? fromDate : LocalDate.now().minusDays(30)).atStartOfDay();
        LocalDateTime end = (toDate != null ? toDate : LocalDate.now()).atTime(LocalTime.MAX);

        long totalRegistered = classRegistrationRepository.countByStatusBetween("REGISTERED", start, end);
        long totalCancelled = classRegistrationRepository.countByStatusBetween("CANCELLED", start, end);

        List<Object[]> rawList = classRegistrationRepository.getClassOccupancyStats();
        List<ClassOccurpancyDetailDTO> details = new ArrayList<>();
        double totalRateSum = 0;

        for (Object[] row : rawList) {
            Long classId = ((Number) row[0]).longValue();
            String subject = (String) row[1];
            String coach = (String) row[2];
            String room = (String) row[3];
            int maxCap = ((Number) row[4]).intValue();
            long registeredCount = row[5] != null ? ((Number) row[5]).longValue() : 0L;

            int available = Math.max(0, maxCap - (int) registeredCount);
            double rate = maxCap > 0 ? ((double) registeredCount / maxCap) * 100 : 0;
            totalRateSum += rate;

            String status = "AVAILABLE";
            if (registeredCount >= maxCap) status = "FULL";
            else if (rate <= 30.0) status = "LOW_ATTENDANCE";

            details.add(ClassOccurpancyDetailDTO.builder()
                    .classId(classId)
                    .coachName(coach)
                    .subjectName(subject)
                    .roomName(room)
                    .maxCapacity(maxCap)
                    .registeredCnt(registeredCount)
                    .availableSlots(available)
                    .occupancyRate(Math.round(rate * 100.0) / 100.0)
                    .status(status)
                    .build()
            );
        }

        double avgRate = details.isEmpty() ? 0 : totalRateSum / details.size();

        return ClassRegistrationReportDTO.builder()
                .totalOpeningClasses(details.size())
                .totalRegistration(totalRegistered)
                .totalCancellation(totalCancelled)
                .averageOccupancyRate(Math.round(avgRate * 100.0) / 100.0)
                .classDetails(details)
                .build();
    }

    @Override
    public RevenueReportDTO getRevenueReport(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = (fromDate != null ? fromDate : LocalDate.now().minusDays(30)).atStartOfDay();
        LocalDateTime end = (toDate != null ? toDate : LocalDate.now()).atTime(LocalTime.MAX);

        BigDecimal totalRevenue = paymentRepository.sumReveneBetween(start, end);
        long totalTransactions = paymentRepository.cntSuccessfulPaymentsBetween(start, end);

        List<Object[]> rawDaily = paymentRepository.getRevenueByDay(start, end);
        List<TimeSeriesDataPoint> timeSeries = rawDaily.stream().map(row -> TimeSeriesDataPoint.builder()
                .timeLabel((String) row[0])
                .value(new BigDecimal(row[1].toString()))
                .build()
        ).toList();

        List<Object[]> rawPkg = paymentRepository.getRevenueByPackage(start, end);
        List<CategoryRevenueDTO> byPackage = mapCategoryRevenue(rawPkg, totalRevenue);

        List<Object[]> rawMethod = paymentRepository.getRevenueByPaymentMethod(start, end);
        List<CategoryRevenueDTO> byMethod = mapCategoryRevenue(rawMethod, totalRevenue);

        return RevenueReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .revenueOvertime(timeSeries)
                .revenueByPackage(byPackage)
                .revenueByMethod(byMethod)
                .build();
    }

    private List<CategoryRevenueDTO> mapCategoryRevenue(List<Object[]> rawData, BigDecimal totalRevenue) {
        List<CategoryRevenueDTO> result = new ArrayList<>();
        for (Object[] row : rawData) {
            String name = (String) row[0];
            BigDecimal amount = new BigDecimal(row[1].toString());
            long count = ((Number) row[2]).longValue();
            double percentage = 0.0;

            if (totalRevenue != null && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }

            result.add(CategoryRevenueDTO.builder()
                    .categoryName(name != null ? name : "Khác")
                    .totalAmount(amount)
                    .transactionCnt(count)
                    .percentage(percentage)
                    .build());
        }
        return result;
    }
}
