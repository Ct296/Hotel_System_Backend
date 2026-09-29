package com.hotel.system.entity;

import com.hotel.system.entity.enums.BillType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "BILL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Bill {
    @Id @Column(name = "BILL_ID", length = 10)
    private String id;

    @Column(name = "BILL_CreateDate", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "BILL_TotalAmount", nullable = false)
    private Double totalAmount;

    @Column(name = "BILL_ActualStayHours")
    private Integer actualStayHours;

    @Column(name = "BILL_ActualRoomAmount")
    private Double actualRoomAmount;

    @Column(name = "BILL_EarlyCheckoutPenaltyPercent")
    private Double earlyCheckoutPenaltyPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "BILL_Type", nullable = false)
    private BillType type;

    @ManyToOne
    @JoinColumn(name = "RENTAL_ID", nullable = false)
    private Rental rental;
}