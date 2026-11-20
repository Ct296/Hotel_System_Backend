package com.hotel.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hotel.system.entity.enums.RentalDetailStatus;
import com.hotel.system.entity.enums.RentalType;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RENTAL_DETAIL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalDetail {
    @Id
    @Column(name = "RENTAL_DETAIL_ID", length = 10)
    private String id;

    @Column(name = "RENTAL_DETAIL_CheckinDate", nullable = false)
    private LocalDateTime checkinDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "RENTAL_DETAIL_Type", length = 20, nullable = false)
    private RentalType rentalType = RentalType.HOURLY;

    @Column(name = "RENTAL_DETAIL_LengthOfStay")
    private Integer lengthOfStay;

    @Column(name = "RENTAL_DETAIL_UnitPrice", nullable = false)
    private Double unitPrice;

        @Column(name = "RENTAL_DETAIL_AdultCount", nullable = false)
    private Integer adultCount;

    @Column(name = "RENTAL_DETAIL_ChildrenCount", nullable = false)
    private Integer childrenCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "RENTAL_DETAIL_Status", length = 20, nullable = false)
    private RentalDetailStatus status;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "RENTAL_ID", referencedColumnName = "RENTAL_ID", nullable = false)
    private Rental rental;

    @ManyToOne
    @JoinColumn(name = "ROOM_ID", referencedColumnName = "ROOM_ID", nullable = false)
    private Room room;

    @OneToMany(mappedBy = "rentalDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceUsage> serviceUsages = new ArrayList<>();

    @OneToMany(mappedBy = "rentalDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Incident> incidents = new ArrayList<>();

    @OneToMany(mappedBy = "rentalDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalGuest> rentalGuests = new ArrayList<>();

    @Column(name = "RENTAL_DETAIL_PreviousDetailId", length = 10)
    private String previousDetailId;

    @Column(name = "RENTAL_DETAIL_Note", length = 255)
    private String note;
}