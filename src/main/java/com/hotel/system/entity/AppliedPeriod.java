package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "APPLIED_PERIOD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppliedPeriod {

    @Id
    @Column(name = "APPLIED_PERIOD_ID", length = 10)
    private String id;

    @Column(name = "APPLIED_PERIOD_StartDate", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "APPLIED_PERIOD_EndDate", nullable = false)
    private LocalDateTime endDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "PRICE_RATE_ID", nullable = false)
    private PriceRate priceRate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ROOM_TYPE_ID", nullable = false)
    private RoomType roomType;
}