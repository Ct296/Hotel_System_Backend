package com.hotel.system.entity;

import com.hotel.system.entity.enums.RentalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RENTAL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rental {

    @Id
    @Column(name = "RENTAL_ID", length = 10)
    private String id;

    @Column(name = "RENTAL_RentDate")
    private LocalDateTime rentDate;

    @Column(name = "RENTAL_Note")
    private String note;

    @Column(name = "RENTAL_IsBooking")
    private Boolean isBooking;

    @Enumerated(EnumType.STRING)
    @Column(name = "RENTAL_Status", length = 20)
    private RentalStatus status;

    @ManyToOne
    @JoinColumn(name = "CUSTOMER_ID", referencedColumnName = "USER_ID")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "STAFF_ID", referencedColumnName = "USER_ID")
    private Staff staff;

    @OneToMany(mappedBy = "rental", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalDetail> rentalDetails = new ArrayList<>();
    
}