package com.hotel.system.dto.vnpay;

import com.hotel.system.entity.enums.BillType;
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
public class VnpayCreatePaymentRequest {

    private String txnRef;
    private String rentalId;
    private BillType billType;
    private long amount;
    private String orderInfo;
    private String ipAddress;
    private String returnUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
}
