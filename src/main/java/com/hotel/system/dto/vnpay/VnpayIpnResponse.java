package com.hotel.system.dto.vnpay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VnpayIpnResponse {

    @JsonProperty("RspCode")
    private String rspCode;

    @JsonProperty("Message")
    private String message;
}
