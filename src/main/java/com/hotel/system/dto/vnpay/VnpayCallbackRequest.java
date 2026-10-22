package com.hotel.system.dto.vnpay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayCallbackRequest {

    private String secureHash;
    private String txnRef;
    private String transactionNo;
    private String bankCode;
    private String bankTranNo;
    private String cardType;
    private String orderInfo;
    private String responseCode;
    private String transactionStatus;
    private String payDate;
    private String amount;
}
