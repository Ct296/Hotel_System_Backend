package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TIER_HISTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TierHistory {

    @Id
    @Column(name = "TIER_HISTORY_ID", length = 10)
    private String id;

    @Column(name = "TIER_HISTORY_StartDate", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "TIER_HISTORY_EndDate")
    private LocalDateTime endDate;

    @Column(name = "TIER_HISTORY_TotalSpending", nullable = false)
    private Double totalSpending;

    @Column(name = "TIER_HISTORY_Reason", nullable = false, length = 100)
    private String reason;

    @ManyToOne(optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private Customer customer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "TIER_CUS_ID", nullable = false)
    private TierCustomer tierCustomer;
}