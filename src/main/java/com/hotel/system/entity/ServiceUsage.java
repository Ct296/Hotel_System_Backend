package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SERVICE_USAGE")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ServiceUsage {
    @Id
    @Column(name = "SERVICE_USAGE_ID", length = 10)
    private String id;

    @Column(name = "SERVICE_USAGE_Count", nullable = false)
    private Integer count;

    @Column(name = "SERVICE_USAGE_Time", nullable = false)
    private LocalDateTime time;

    @Column(name = "SERVICE_USAGE_UnitPrice", nullable = false)
    private Double unitPrice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "RENTAL_DETAIL_ID", nullable = false)
    private RentalDetail rentalDetail;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SERVICE_ID", nullable = false)
    private Service service;
}