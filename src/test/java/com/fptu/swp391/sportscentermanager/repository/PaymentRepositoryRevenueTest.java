package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.entity.Payment;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.enums.PackageType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Mục 10: doanh thu = tiền thu trong kỳ (theo ngày thu) - tiền hoàn trong kỳ (theo ngày hoàn)
class PaymentRepositoryRevenueTest extends MySqlRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private EntityManager em;

    private static final LocalDateTime NOV_START = LocalDateTime.of(2026, 11, 1, 0, 0);
    private static final LocalDateTime NOV_END = LocalDateTime.of(2026, 11, 30, 23, 59, 59);
    private static final LocalDateTime OCT_START = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime OCT_END = LocalDateTime.of(2026, 10, 31, 23, 59, 59);

    private Subscription subA;
    private Subscription subB;

    // Dữ liệu (tháng 11 trừ khi ghi khác):
    //  p1 gói A, VNPAY: thu 500k ngày 05/11
    //  p2 gói B, CASH : thu 780k ngày 05/11, hoàn 676k ngày 10/11  -> giữ 104k
    //  p3 gói B, CASH : thu 300k ngày 06/11, hoàn đủ 300k cùng ngày -> 0
    //  p4 gói A, CASH : thu 200k ngày 20/10, hoàn đủ ngày 10/11    -> tháng 10 +200k, tháng 11 -200k
    //  p5 PENDING 999k và p6 CANCELLED 888k trong tháng 11          -> không tính
    @BeforeEach
    void seed() {
        Member member = Member.builder()
            .firstName("An").lastName("Nguyen").gender("Nam")
            .email("an@test.com").phone("0900000001").passwordHash("x")
            .trainingGoal("Giảm cân").createdAt(LocalDateTime.now())
            .build();
        em.persist(member);
        subA = subscription(member, "Gói A");
        subB = subscription(member, "Gói B");

        paid(subA, "VNPAY", 500_000, "COMPLETED", at(11, 5), null, null);
        paid(subB, "CASH", 780_000, "REFUNDED", at(11, 5), 676_000L, at(11, 10));
        paid(subB, "CASH", 300_000, "REFUNDED", at(11, 6), 300_000L, at(11, 6));
        paid(subA, "CASH", 200_000, "REFUNDED", at(10, 20), 200_000L, at(11, 10));
        paid(subA, "VNPAY", 999_000, "PENDING", at(11, 7), null, null);
        paid(subA, "VNPAY", 888_000, "CANCELLED", at(11, 7), null, null);
        em.flush();
        em.clear();
    }

    private static LocalDateTime at(int month, int day) {
        return LocalDateTime.of(2026, month, day, 10, 0);
    }

    private Subscription subscription(Member member, String packageName) {
        MembershipPackage pkg = MembershipPackage.builder()
            .packageName(packageName).packageType(PackageType.MEMBERSHIP).price(1).durationDays(30)
            .build();
        em.persist(pkg);
        Subscription sub = Subscription.builder().member(member).membershipPackage(pkg).status("ACTIVE").build();
        em.persist(sub);
        return sub;
    }

    private void paid(Subscription sub, String method, long amount, String status,
                      LocalDateTime paidAt, Long refundAmount, LocalDateTime refundedAt) {
        em.persist(Payment.builder()
            .subscription(sub).paymentMethod(method).amount(BigDecimal.valueOf(amount)).status(status)
            .paymentDate(paidAt).paidAt(paidAt)
            .refundAmount(refundAmount == null ? null : BigDecimal.valueOf(refundAmount)).refundedAt(refundedAt)
            .build());
    }

    private static void assertMoney(long expected, Object actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo((BigDecimal) actual),
            "mong đợi " + expected + " nhưng là " + actual);
    }

    @Test
    @DisplayName("Tổng doanh thu tháng 11: 500k + 104k + 0 - 200k = 404k")
    void sumRevenue_partialRefundKeepsRemainder() {
        assertMoney(404_000, paymentRepository.sumReveneBetween(NOV_START, NOV_END));
    }

    @Test
    @DisplayName("Tiền hoàn ở tháng sau không làm đổi doanh thu tháng thu tiền")
    void sumRevenue_refundBookedInRefundPeriod() {
        assertMoney(200_000, paymentRepository.sumReveneBetween(OCT_START, OCT_END));
    }

    @Test
    @DisplayName("Số giao dịch đếm theo ngày thu, kể cả giao dịch sau đó bị hoàn")
    void countPayments() {
        assertEquals(3, paymentRepository.cntSuccessfulPaymentsBetween(NOV_START, NOV_END));
        assertEquals(1, paymentRepository.cntSuccessfulPaymentsBetween(OCT_START, OCT_END));
    }

    @Test
    @DisplayName("Doanh thu theo ngày (native query MySQL): dòng thu và dòng hoàn rơi đúng ngày")
    void revenueByDay() {
        Map<String, Object> byDay = paymentRepository.getRevenueByDay(NOV_START, NOV_END).stream()
            .collect(Collectors.toMap(r -> (String) r[0], r -> r[1]));

        assertEquals(List.of("2026-11-05", "2026-11-06", "2026-11-10"), byDay.keySet().stream().sorted().toList());
        assertMoney(1_280_000, byDay.get("2026-11-05"));
        assertMoney(0, byDay.get("2026-11-06"));
        assertMoney(-876_000, byDay.get("2026-11-10"));
    }

    @Test
    @DisplayName("Doanh thu theo gói: trừ tiền hoàn đúng gói, số giao dịch theo ngày thu")
    void revenueByPackage() {
        Map<String, Object[]> rows = paymentRepository.getRevenueByPackage(NOV_START, NOV_END).stream()
            .collect(Collectors.toMap(r -> (String) r[0], r -> r));

        assertMoney(300_000, rows.get("Gói A")[1]);   // 500k - 200k hoàn của p4
        assertEquals(1, ((Number) rows.get("Gói A")[2]).longValue());
        assertMoney(104_000, rows.get("Gói B")[1]);
        assertEquals(2, ((Number) rows.get("Gói B")[2]).longValue());
    }

    @Test
    @DisplayName("Doanh thu theo phương thức thanh toán")
    void revenueByPaymentMethod() {
        Map<String, Object[]> rows = paymentRepository.getRevenueByPaymentMethod(NOV_START, NOV_END).stream()
            .collect(Collectors.toMap(r -> (String) r[0], r -> r));

        assertMoney(500_000, rows.get("VNPAY")[1]);
        assertEquals(1, ((Number) rows.get("VNPAY")[2]).longValue());
        assertMoney(-96_000, rows.get("CASH")[1]);    // 104k + 0 - 200k
        assertEquals(2, ((Number) rows.get("CASH")[2]).longValue());
    }
}
