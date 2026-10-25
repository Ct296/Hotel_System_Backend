package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "PRICE_RATE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceRate {

    @Id
    @Column(name = "PRICE_RATE_ID", length = 10)
    private String id;

    @Column(name = "PRICE_RATE_EventName", nullable = false, length = 50)
    private String eventName;

    @Column(name = "PRICE_RATE_SurchargeAmount", nullable = false)
    private Double surchargeAmount;

    @Column(name = "PRICE_RATE_CreateDate", nullable = false)
    private LocalDateTime createDate;
    
    @Column(name = "PRICE_RATE_UpdateDate")
    private LocalDateTime updateDate;

    @OneToMany(mappedBy = "priceRate")
    @JsonIgnore
    private List<AppliedPeriod> appliedPeriods;
}