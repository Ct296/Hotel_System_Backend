package com.hotel.system.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "payment.vnpay")
@Validated
@Getter
@Setter
public class VnpayProperties {
    private boolean enabled = false;
    private String tmnCode;
    private String hashSecret;
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String returnUrl;
    private String ipnUrl;
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private String locale = "vn";
    private String currCode = "VND";
    @Min(1) private int timeoutMinutes = 30;
    private String defaultIpAddress = "127.0.0.1";

    public String getTmnCode() {
        return normalize(tmnCode);
    }

    public String getHashSecret() {
        return normalize(hashSecret);
    }

    public String getPayUrl() {
        return normalize(payUrl);
    }

    public String getReturnUrl() {
        return normalize(returnUrl);
    }

    public String getIpnUrl() {
        return normalize(ipnUrl);
    }

    public String getVersion() {
        return normalize(version);
    }

    public String getCommand() {
        return normalize(command);
    }

    public String getOrderType() {
        return normalize(orderType);
    }

    public String getLocale() {
        return normalize(locale);
    }

    public String getCurrCode() {
        return normalize(currCode);
    }

    public String getDefaultIpAddress() {
        return normalize(defaultIpAddress);
    }

    public boolean isConfigured() {
        return enabled
                && StringUtils.hasText(getTmnCode())
                && StringUtils.hasText(getHashSecret())
                && StringUtils.hasText(getPayUrl())
                && StringUtils.hasText(getReturnUrl())
                && StringUtils.hasText(getIpnUrl());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
