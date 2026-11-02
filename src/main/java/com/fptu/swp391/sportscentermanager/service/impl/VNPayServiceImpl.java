package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.config.VNPayConfig;
import com.fptu.swp391.sportscentermanager.dto.VNPayCallBackResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.VNPayPaymentResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.SubscriptionStatus;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import com.fptu.swp391.sportscentermanager.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {
    private final VNPayConfig vnPayConfig;
    private final MemberRepository memberRepository;
    private final MembershipPackageRepository packageRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public VNPayPaymentResponseDTO createPaymentUrl(Long memberId, Long packageId, HttpServletRequest request) {
        Member member = memberRepository.findByIdForUpdate(memberId).orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        MembershipPackage pkg = packageRepository.findByPackageIdAndStatus(packageId, "ACTIVE").orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        subscriptionService.requireMembershipForClass(memberId, pkg, now);

        subscriptionService.cancelStalePendingOrThrow(memberId, now);

        Subscription subscription = subscriptionRepository.save(Subscription.builder().member(member).membershipPackage(pkg).status(SubscriptionStatus.PENDING.name()).build());

        // tao payemnt o trang thai pending
        Payment payment = paymentRepository.save(Payment.builder().subscription(subscription).amount(BigDecimal.valueOf(pkg.getPrice())).paymentMethod("VNPAY").paymentDate(now).status("PENDING").build());

        // gom tham so VNPay (nhan 100 theo chuan cua VNPay
        long amountInVND = (long) (pkg.getPrice() * 100);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String vnpCreateDate = now.format(formatter);
        String vnpExpireDate = now.plusMinutes(15).format(formatter);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getVnpTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amountInVND));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", String.valueOf(payment.getPaymentId()));
        vnpParams.put("vnp_OrderInfo", "Thanh toan goi tap " + pkg.getPackageId());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getVnpReturnUrl());
        vnpParams.put("vnp_IpAddr", VNPayConfig.getIpAddress(request));
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // Sap xep theo alphabet va noi querry string
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        try {
            for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext(); ) {
                String fieldName = itr.next();
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString())).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                        query.append('&');
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Lỗi encode VNPay query: " + e.getMessage());
        }

        String queryUrl = query.toString();
        String vnpSecureHash = VNPayConfig.hmacSHA512(vnPayConfig.getVnpHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        String paymentUrl = vnPayConfig.getVnpPayUrl() + "?" + queryUrl;

        return VNPayPaymentResponseDTO.builder()
            .paymentUrl(paymentUrl)
            .paymentId(payment.getPaymentId())
            .subscriptionId(subscription.getSubscriptionId())
            .build();
    }

    @Override
    @Transactional
    public VNPayCallBackResponseDTO processCallBack(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }
        String vnpSecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");
        String signValue;
        try {
            signValue = VNPayConfig.hashAllFields(fields, vnPayConfig.getVnpHashSecret());
        } catch (UnsupportedEncodingException e) {
            return VNPayCallBackResponseDTO.builder()
                .success(false)
                .message("Lỗi đối soát chữ ký VNPay")
                .rspCode("99")
                .build();
        }

        if (!signValue.equalsIgnoreCase(vnpSecureHash)) {
            return VNPayCallBackResponseDTO.builder()
                .success(false)
                .message("Chữ ký VNPay không hợp lệ")
                .rspCode("97")
                .build();
        }
        Long paymentId = Long.parseLong(request.getParameter("vnp_TxnRef"));
        // Khoá dòng payment: IPN và return URL có thể tới cùng lúc, chỉ 1 request được xử lý
        Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElse(null);
        if (payment == null) {
            return VNPayCallBackResponseDTO.builder()
                .success(false)
                .message("Không tìm thấy giao dịch")
                .rspCode("01")
                .build();
        }

        if (!"PENDING".equals(payment.getStatus())) {
            if ("CANCELLED".equals(payment.getStatus()) && "00".equals(request.getParameter("vnp_ResponseCode"))) {
                log.error("VNPay báo đã thu tiền cho giao dịch đã huỷ: paymentId={}, vnp_TransactionNo={}",
                    paymentId, request.getParameter("vnp_TransactionNo"));
                List<User> managers = userRepository.findByRole_RoleNameAndStatus("CENTER_MANAGER", UserStatus.ACTIVE);
                if (!managers.isEmpty()) {
                    notificationService.notifyUsers(
                        managers,
                        "Cảnh báo giao dịch VNPay thu tiền sau khi huỷ",
                        "Giao dịch ID #" + paymentId + " (mã GD VNPay: " + request.getParameter("vnp_TransactionNo")
                            + ") đã bị huỷ trước đó nhưng VNPay vừa báo thành công. Vui lòng kiểm tra đối soát và hoàn tiền tay cho khách hàng.",
                        "PAYMENT_ALERT"
                    );
                }
            }
            return VNPayCallBackResponseDTO.builder()
                .success("COMPLETED".equals(payment.getStatus()))
                .message("Giao dịch đã được xử lý trước đó")
                .paymentId(paymentId)
                .rspCode("02")
                .build();
        }

        long vnpAmount = Long.parseLong(request.getParameter("vnp_Amount"));
        long expectedAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (vnpAmount != expectedAmount) {
            return VNPayCallBackResponseDTO.builder()
                .success(false)
                .message("Số tiền thanh toán không hợp lệ")
                .paymentId(paymentId)
                .rspCode("04")
                .build();
        }

        String responseCode = request.getParameter("vnp_ResponseCode");
        String transactionNo = request.getParameter("vnp_TransactionNo");

        if ("00".equals(responseCode)) {
            LocalDateTime paidAt = LocalDateTime.now();
            payment.setStatus("COMPLETED");
            payment.setPaidAt(paidAt);
            payment.setTransactionRef(transactionNo);
            paymentRepository.save(payment);

            subscriptionService.markPaid(payment.getSubscription(), paidAt);

            return VNPayCallBackResponseDTO.builder()
                .success(true)
                .message("Thanh toán thành công!")
                .paymentId(paymentId)
                .transactionNo(transactionNo)
                .responseCode(responseCode)
                .rspCode("00")
                .build();
        } else {
            // Thanh toán bị hủy hoặc thất bại
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            Subscription subscription = payment.getSubscription();
            subscription.setStatus("CANCELLED");
            subscriptionRepository.save(subscription);

            return VNPayCallBackResponseDTO.builder()
                .success(false)
                .message("Thanh toán thất bại hoặc đã bị hủy!")
                .paymentId(paymentId)
                .transactionNo(transactionNo)
                .responseCode(responseCode)
                // 00 = merchant đã ghi nhận kết quả, không có nghĩa là thanh toán thành công
                .rspCode("00")
                .build();
        }
    }

}
