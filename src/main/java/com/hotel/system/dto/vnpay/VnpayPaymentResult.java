package com.hotel.system.dto.vnpay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayPaymentResult {

    private boolean validSignature;
    private boolean success;
    private String txnRef;
    private Long amount;
    private String responseCode;
    private String transactionStatus;
    private String transactionNo;
    private String bankCode;
    private String bankTranNo;
    private String cardType;
    private String orderInfo;
    private String payDateRaw;
    private LocalDateTime paidAt;
}
