package com.hotel.system.entity;

import com.hotel.system.entity.enums.RentalDetailStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RENTAL_DETAIL")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RentalDetail {
    @Id
    @Column(name = "RENTAL_DETAIL_ID", length = 10)
    private String id;

    @Column(name = "RENTAL_DETAIL_CheckinDate", nullable = false)
    private LocalDateTime checkinDate;

    @Column(name = "RENTAL_DETAIL_LengthOfStay", nullable = false)
    private Integer lengthOfStay;

    @Column(name = "RENTAL_DETAIL_ChildrenCount", nullable = false)
    private Integer childrenCount;

    @Column(name = "RENTAL_DETAIL_AdultCount", nullable = false)
    private Integer adultCount;

    @Column(name = "RENTAL_DETAIL_UnitPrice", nullable = false)
    private Double unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "RENTAL_DETAIL_Status", nullable = false, length = 20)
    private RentalDetailStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "RENTAL_ID", nullable = false)
    private Rental rental;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ROOM_ID", nullable = false)
    private Room room;

    @OneToMany(mappedBy = "rentalDetail", cascade = CascadeType.ALL)
    private List<ServiceUsage> serviceUsages = new ArrayList<>();

    @OneToMany(mappedBy = "rentalDetail", cascade = CascadeType.ALL)
    private List<Incident> incidents = new ArrayList<>();
}