package com.fptu.swp391.sportscentermanager.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
@Setter
@Configuration
public class VNPayConfig {
    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.pay-url}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    // hash hmac512 chuan theo yeu cau cua vnpay
    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null) {
                throw new NullPointerException();
            }
            Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmacSHA512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmacSHA512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    // lay ip client
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }


        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || ipAddress == null) {
            ipAddress = "127.0.0.1";
        }

        return ipAddress;
    }

    // sort cac truong thro alphabet va tao chuoi hash
    public static String hashAllFields(Map<String, String> fields, String secretKey) throws UnsupportedEncodingException {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder sb = new StringBuilder();
        for (String fieldName : fieldNames) {
            if (fieldName != null && !fieldName.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(fieldName);
                sb.append("=");
                sb.append(URLEncoder.encode(fields.get(fieldName), StandardCharsets.US_ASCII.toString()));
            }
        }
        return hmacSHA512(secretKey, sb.toString());
    }
}
