package com.hotel.system.entity;

import com.hotel.system.entity.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @Column(name = "PAYMENT_ID", length = 10)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_Method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "PAYMENT_Date", nullable = false)
    private LocalDateTime date;

    @Column(name = "PAYMENT_Transaction", nullable = false, length = 50)
    private String transaction;

    @OneToOne(optional = false)
    @JoinColumn(name = "BILL_ID", nullable = false, unique = true)
    private Bill bill;
}
