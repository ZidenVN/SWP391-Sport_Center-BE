package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.config.VNPayConfig;
import com.fptu.swp391.sportscentermanager.dto.VNPayCallBackResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.VNPayPaymentResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.entity.Payment;
import com.fptu.swp391.sportscentermanager.entity.Subscription;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.MembershipPackageRepository;
import com.fptu.swp391.sportscentermanager.repository.PaymentRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
import com.fptu.swp391.sportscentermanager.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {
    private final VNPayConfig vnPayConfig;
    private final MemberRepository memberRepository;
    private final MembershipPackageRepository packageRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;


    @Override
    @Transactional
    public VNPayPaymentResponseDTO createPaymentUrl(Long memberId, Long packageId, HttpServletRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        MembershipPackage pkg = packageRepository.findById(packageId).orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        // Không cho tạo nhiều giao dịch chờ thanh toán cùng lúc
        for (Subscription pending : subscriptionRepository.findByMember_UserIdAndStatus(memberId, "PENDING")) {
            Payment pendingPayment = paymentRepository.findFirstBySubscription_SubscriptionIdOrderByPaymentDateDesc(pending.getSubscriptionId()).orElse(null);
            boolean stillValid = pendingPayment != null && pendingPayment.getPaymentDate().isAfter(now.minusMinutes(15));
            if (stillValid) {
                throw new AppException(ErrorCode.PAYMENT_PENDING_EXISTS);
            }

            pending.setStatus("CANCELLED");
            if (pendingPayment != null)
                pendingPayment.setStatus("CANCELLED");
        }


        LocalDateTime startDate = calculateStartDate(memberId, now);
        LocalDateTime endDate = startDate.plusDays(pkg.getDurationDays());

        Subscription subscription = subscriptionRepository.save(Subscription.builder().member(member).membershipPackage(pkg).startDate(startDate).endDate(endDate).status("PENDING").build());

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
        // Tùy chọn: FE chọn sẵn phương thức để VNPay bỏ qua màn chọn (VNBANK = ATM nội địa, INTCARD = thẻ quốc tế, VNPAYQR = QR)
        String bankCode = request.getParameter("bankCode");
        if (bankCode != null && Set.of("VNBANK", "INTCARD", "VNPAYQR").contains(bankCode)) {
            vnpParams.put("vnp_BankCode", bankCode);
        }
        vnpParams.put("vnp_ReturnUrl", resolveReturnUrl(request.getParameter("returnUrl")));
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
                .build();
        }

        if (!signValue.equalsIgnoreCase(vnpSecureHash)) {
            return VNPayCallBackResponseDTO.builder()
                .success(false)
                .message("Chữ ký VNPay không hợp lệ")
                .build();
        }
        Long paymentId = Long.parseLong(request.getParameter("vnp_TxnRef"));
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!"PENDING".equals(payment.getStatus())) {
            return VNPayCallBackResponseDTO.builder()
                .success("COMPLETED".equals(payment.getStatus()))
                .message("Giao dịch đã được xử lý trước đó")
                .paymentId(paymentId)
                .build();
        }

        long vnpAmount = Long.parseLong(request.getParameter("vnp_Amount"));
        long expectedAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (vnpAmount != expectedAmount) {
            return VNPayCallBackResponseDTO.builder()
                .success(false)
                .message("Số tiền thanh toán không hợp lệ")
                .paymentId(paymentId)
                .build();
        }

        String responseCode = request.getParameter("vnp_ResponseCode");
        String transactionNo = request.getParameter("vnp_TransactionNo");

        if ("00".equals(responseCode)) {
            payment.setStatus("COMPLETED");
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);


            Subscription subscription = payment.getSubscription();
            LocalDateTime startDate = calculateStartDate(subscription.getMember().getUserId(), LocalDateTime.now());
            subscription.setStartDate(startDate);
            subscription.setEndDate(startDate.plusDays(subscription.getMembershipPackage().getDurationDays()));
            subscription.setStatus("ACTIVE");
            subscriptionRepository.save(subscription);

            return VNPayCallBackResponseDTO.builder()
                .success(true)
                .message("Thanh toán thành công!")
                .paymentId(paymentId)
                .transactionNo(transactionNo)
                .responseCode(responseCode)
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
                .build();
        }
    }

    // FE có thể xin return URL khác (vd: thanh toán bằng điện thoại khi quét QR cần IP LAN thay cho localhost).
    // Chỉ chấp nhận trang /payment-result trên cùng host đã cấu hình hoặc trong mạng nội bộ, tránh open redirect.
    private String resolveReturnUrl(String requested) {
        String configured = vnPayConfig.getVnpReturnUrl();
        if (requested == null || requested.isBlank()) return configured;
        try {
            java.net.URI uri = java.net.URI.create(requested);
            String host = uri.getHost();
            boolean validScheme = "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
            boolean validPath = uri.getPath() != null && uri.getPath().endsWith("/payment-result");
            boolean sameHost = host != null && host.equalsIgnoreCase(java.net.URI.create(configured).getHost());
            boolean privateLan = host != null && (host.equals("localhost") || host.equals("127.0.0.1")
                || host.startsWith("192.168.") || host.startsWith("10.") || host.matches("172\\.(1[6-9]|2\\d|3[01])\\..*"));
            if (validScheme && validPath && uri.getQuery() == null && (sameHost || privateLan)) return requested;
        } catch (IllegalArgumentException ignored) {
            // URL sai định dạng -> dùng cấu hình mặc định
        }
        return configured;
    }

    private LocalDateTime calculateStartDate(Long memberId, LocalDateTime now) {
        return subscriptionRepository.findFirstByMember_UserIdAndStatusOrderByEndDateDesc(memberId, "ACTIVE").map(Subscription::getEndDate).filter(d -> d.isAfter(now)).orElse(now);
    }
}
