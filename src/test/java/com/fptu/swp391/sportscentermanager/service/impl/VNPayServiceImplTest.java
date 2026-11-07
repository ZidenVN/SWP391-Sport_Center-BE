package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.config.VNPayConfig;
import com.fptu.swp391.sportscentermanager.dto.VNPayCallBackResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.Payment;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Mục 25: VNPay báo đã thu tiền (IPN/return URL tới muộn) cho payment đã bị huỷ
@ExtendWith(MockitoExtension.class)
class VNPayServiceImplTest {

    @Mock
    private VNPayConfig vnPayConfig;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MembershipPackageRepository packageRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VNPayServiceImpl vnPayService;

    private static final String SECRET = "test-secret";
    private static final Long PAYMENT_ID = 7L;

    @BeforeEach
    void setUp() {
        when(vnPayConfig.getVnpHashSecret()).thenReturn(SECRET);
    }

    // Request có chữ ký đúng, giống VNPay gửi sang
    private MockHttpServletRequest signedRequest(String responseCode) throws Exception {
        Map<String, String> fields = new TreeMap<>(Map.of(
            "vnp_TxnRef", String.valueOf(PAYMENT_ID),
            "vnp_Amount", "59999900",
            "vnp_ResponseCode", responseCode,
            "vnp_TransactionNo", "15728841"
        ));
        MockHttpServletRequest request = new MockHttpServletRequest();
        fields.forEach(request::addParameter);
        request.addParameter("vnp_SecureHash", VNPayConfig.hashAllFields(new TreeMap<>(fields), SECRET));
        return request;
    }

    private Payment payment(String status, String note) {
        Payment p = Payment.builder()
            .paymentId(PAYMENT_ID)
            .amount(BigDecimal.valueOf(599_999))
            .status(status)
            .note(note)
            .build();
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    @DisplayName("Payment đã huỷ mà VNPay báo thu tiền: trả 02, ghi dấu và báo quản lý")
    void cancelledButPaid_notifiesManagers() throws Exception {
        Payment p = payment("CANCELLED", null);
        List<User> managers = List.of(Member.builder().userId(99L).build());
        when(userRepository.findByRole_RoleNameAndStatus("CENTER_MANAGER", UserStatus.ACTIVE)).thenReturn(managers);

        VNPayCallBackResponseDTO res = vnPayService.processCallBack(signedRequest("00"));

        assertEquals("02", res.getRspCode());
        assertFalse(res.getSuccess());
        assertEquals("CANCELLED", p.getStatus(), "không được tự kích hoạt lại payment đã huỷ");
        assertEquals("VNPAY_PAID_AFTER_CANCEL", p.getNote());
        verify(notificationService).notifyUsers(eq(managers), anyString(), contains("15728841"), eq("PAYMENT_NEEDS_REVIEW"));
    }

    @Test
    @DisplayName("IPN gọi lại lần 2 cho cùng giao dịch: không báo quản lý thêm lần nữa")
    void cancelledButPaid_secondCall_noDuplicateNotification() throws Exception {
        payment("CANCELLED", "VNPAY_PAID_AFTER_CANCEL");

        VNPayCallBackResponseDTO res = vnPayService.processCallBack(signedRequest("00"));

        assertEquals("02", res.getRspCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Payment đã huỷ và VNPay báo khách huỷ (mã 24): không báo quản lý")
    void cancelledAndNotPaid_noNotification() throws Exception {
        payment("CANCELLED", null);

        VNPayCallBackResponseDTO res = vnPayService.processCallBack(signedRequest("24"));

        assertEquals("02", res.getRspCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Payment đã COMPLETED (IPN và return URL cùng tới): trả 02, success, không báo ai")
    void alreadyCompleted_returnsProcessed() throws Exception {
        payment("COMPLETED", null);

        VNPayCallBackResponseDTO res = vnPayService.processCallBack(signedRequest("00"));

        assertEquals("02", res.getRspCode());
        assertTrue(res.getSuccess());
        verifyNoInteractions(notificationService);
    }
}
