package com.hotel.system.dto.billing;

import com.hotel.system.entity.enums.BillType;

public record BillingSummary(
        double roomAmount,
        double serviceAmount,
        double depositPaid,
        double depositAmount,
        double finalAmount,
        int actualStayHours,
        
        double earlyCheckinSurcharge,
        double lateCheckoutSurcharge,
        
        double completionRatio,
        BillType billType
) {

    public Object earlyCheckoutPenaltyPercent() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'earlyCheckoutPenaltyPercent'");
    }}