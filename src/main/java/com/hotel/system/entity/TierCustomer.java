package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "TIER_CUSTOMER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TierCustomer {

    @Id
    @Column(name = "TIER_CUS_ID", length = 10)
    private String id;

    @Column(name = "TIER_CUS_Name", nullable = false, length = 30)
    private String name;

    @Column(name = "TIER_CUS_Condition", nullable = false)
    private Double condition;

    @Column(name = "TIER_CUS_Benefit", length = 255)
    private String benefit;

    @Column(name = "TIER_CUS_Discount", precision = 5, scale = 2)
    private BigDecimal discount;

}