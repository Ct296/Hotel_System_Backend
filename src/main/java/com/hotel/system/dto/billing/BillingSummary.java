package com.hotel.system.dto.billing;

import com.hotel.system.entity.enums.BillType;

public record BillingSummary(
        double roomAmount,
        double serviceAmount,
        double depositPaid,
        double depositAmount,
        double finalAmount,
        int actualStayHours,
        double earlyCheckoutPenaltyPercent,
        double completionRatio,
        BillType billType
) {}