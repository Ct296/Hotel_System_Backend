package com.hotel.system.service;

import com.hotel.system.config.VnpayProperties;
import com.hotel.system.dto.vnpay.VnpayCallbackRequest;
import com.hotel.system.dto.vnpay.VnpayCreatePaymentRequest;
import com.hotel.system.dto.vnpay.VnpayPaymentResult;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class VnpayService {

    private static final DateTimeFormatter VNPAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties vnpayProperties;

    public VnpayService(VnpayProperties vnpayProperties) {
        this.vnpayProperties = vnpayProperties;
    }

    public boolean isReady() {
        return vnpayProperties.isConfigured();
    }

    public String buildPaymentUrl(VnpayCreatePaymentRequest request) {
        if (!isReady()) {
            throw new IllegalStateException("Cấu hình VNPay chưa đầy đủ.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu tạo thanh toán không hợp lệ.");
        }
        if (!StringUtils.hasText(request.getTxnRef())) {
            throw new IllegalArgumentException("Thiếu mã giao dịch VNPay.");
        }
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
        }

        LocalDateTime createdAt = request.getCreatedAt() == null ? LocalDateTime.now() : request.getCreatedAt();
        LocalDateTime expiredAt = request.getExpiredAt() == null
                ? createdAt.plusMinutes(vnpayProperties.getTimeoutMinutes())
                : request.getExpiredAt();

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnpayProperties.getVersion());
        params.put("vnp_Command", vnpayProperties.getCommand());
        params.put("vnp_TmnCode", vnpayProperties.getTmnCode());
        params.put("vnp_Amount", String.valueOf(request.getAmount() * 100L));
        params.put("vnp_CurrCode", vnpayProperties.getCurrCode());
        params.put("vnp_TxnRef", request.getTxnRef());
        params.put("vnp_OrderInfo", safeValue(request.getOrderInfo()));
        params.put("vnp_OrderType", vnpayProperties.getOrderType());
        params.put("vnp_Locale", vnpayProperties.getLocale());
        params.put("vnp_ReturnUrl", safeValue(request.getReturnUrl()));
        params.put("vnp_IpAddr", resolveIpAddress(request.getIpAddress()));
        params.put("vnp_CreateDate", createdAt.format(VNPAY_DATE_FORMATTER));
        params.put("vnp_ExpireDate", expiredAt.format(VNPAY_DATE_FORMATTER));

        String hashData = buildHashData(params);
        String secureHash = hmacSha512(vnpayProperties.getHashSecret(), hashData);

        return UriComponentsBuilder.fromHttpUrl(vnpayProperties.getPayUrl())
                .query(buildEncodedQuery(params))
                .queryParam("vnp_SecureHash", secureHash)
                .build(false)
                .toUriString();
    }

    public VnpayPaymentResult parsePaymentResult(Map<String, String> requestParams) {
        Map<String, String> safeParams = requestParams == null ? Map.of() : new HashMap<>(requestParams);
        String secureHash = safeParams.get("vnp_SecureHash");
        boolean validSignature = verifySignature(safeParams, secureHash);

        Long amount = null;
        String amountRaw = safeParams.get("vnp_Amount");
        if (StringUtils.hasText(amountRaw)) {
            try {
                amount = Long.parseLong(amountRaw) / 100L;
            } catch (NumberFormatException ignored) {
            }
        }

        String responseCode = safeParams.get("vnp_ResponseCode");
        String transactionStatus = safeParams.get("vnp_TransactionStatus");

        return VnpayPaymentResult.builder()
                .validSignature(validSignature)
                .success(validSignature && "00".equals(responseCode) && "00".equals(transactionStatus))
                .txnRef(safeParams.get("vnp_TxnRef"))
                .amount(amount)
                .responseCode(responseCode)
                .transactionStatus(transactionStatus)
                .transactionNo(safeParams.get("vnp_TransactionNo"))
                .bankCode(safeParams.get("vnp_BankCode"))
                .bankTranNo(safeParams.get("vnp_BankTranNo"))
                .cardType(safeParams.get("vnp_CardType"))
                .orderInfo(safeParams.get("vnp_OrderInfo"))
                .payDateRaw(safeParams.get("vnp_PayDate"))
                .paidAt(parsePayDate(safeParams.get("vnp_PayDate")))
                .build();
    }

    public VnpayCallbackRequest toCallbackRequest(Map<String, String> requestParams) {
        Map<String, String> safeParams = requestParams == null ? Map.of() : requestParams;
        return VnpayCallbackRequest.builder()
                .secureHash(safeParams.get("vnp_SecureHash"))
                .txnRef(safeParams.get("vnp_TxnRef"))
                .transactionNo(safeParams.get("vnp_TransactionNo"))
                .bankCode(safeParams.get("vnp_BankCode"))
                .bankTranNo(safeParams.get("vnp_BankTranNo"))
                .cardType(safeParams.get("vnp_CardType"))
                .orderInfo(safeParams.get("vnp_OrderInfo"))
                .responseCode(safeParams.get("vnp_ResponseCode"))
                .transactionStatus(safeParams.get("vnp_TransactionStatus"))
                .payDate(safeParams.get("vnp_PayDate"))
                .amount(safeParams.get("vnp_Amount"))
                .build();
    }

    public boolean verifySignature(Map<String, String> requestParams, String receivedSecureHash) {
        if (!StringUtils.hasText(receivedSecureHash) || requestParams == null || requestParams.isEmpty()) {
            return false;
        }

        Map<String, String> filteredParams = new TreeMap<>();
        requestParams.forEach((key, value) -> {
            if (StringUtils.hasText(key)
                    && StringUtils.hasText(value)
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)) {
                filteredParams.put(key, value);
            }
        });

        String expectedHash = hmacSha512(vnpayProperties.getHashSecret(), buildHashData(filteredParams));
        return expectedHash.equalsIgnoreCase(receivedSecureHash);
    }

    private String buildHashData(Map<String, String> params) {
        return params.entrySet()
                .stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String buildEncodedQuery(Map<String, String> params) {
        List<String> segments = new ArrayList<>();
        params.forEach((key, value) -> {
            if (StringUtils.hasText(value)) {
                segments.add(encode(key) + "=" + encode(value));
            }
        });
        return String.join("&", segments);
    }

    private LocalDateTime parsePayDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), VNPAY_DATE_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveIpAddress(String ipAddress) {
        return StringUtils.hasText(ipAddress) ? ipAddress.trim() : vnpayProperties.getDefaultIpAddress();
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte aByte : bytes) {
                sb.append(String.format("%02x", aByte));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo chữ ký VNPay.", ex);
        }
    }
}
