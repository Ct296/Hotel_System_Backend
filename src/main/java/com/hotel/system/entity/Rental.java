package com.hotel.system.entity;

import com.hotel.system.entity.enums.RentalStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RENTAL")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Rental {
    @Id
    @Column(name = "RENTAL_ID", length = 10)
    private String id;

    @Column(name = "RENTAL_RentDate", nullable = false)
    private LocalDateTime rentDate;

    @Column(name = "RENTAL_Note", length = 255)
    private String note;

    @Column(name = "RENTAL_IsBooking", nullable = false)
    private Boolean isBooking;

    @Enumerated(EnumType.STRING)
    @Column(name = "RENTAL_Status", nullable = false, length = 20)
    private RentalStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STAFF_ID")
    private Staff staff; // Nullable: Nếu khách tự đặt online thì null

    @OneToMany(mappedBy = "rental", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalDetail> details = new ArrayList<>();

    @OneToMany(mappedBy = "rental")
    private List<Bill> bills = new ArrayList<>();
}