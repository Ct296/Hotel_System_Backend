package com.hotel.system.entity;

import com.hotel.system.entity.enums.RentalDetailStatus;
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

    @Column(name = "RENTAL_DETAIL_CheckinDate")
    private LocalDateTime checkinDate;

    @Column(name = "RENTAL_DETAIL_LengthOfStay")
    private Integer lengthOfStay;

    @Column(name = "RENTAL_DETAIL_UnitPrice")
    private Double unitPrice;

        @Column(name = "RENTAL_DETAIL_AdultCount", nullable = false)
    private Integer adultCount;

    @Column(name = "RENTAL_DETAIL_ChildrenCount", nullable = false)
    private Integer childrenCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "RENTAL_DETAIL_Status", length = 20)
    private RentalDetailStatus status;

    @ManyToOne
    @JoinColumn(name = "RENTAL_ID", referencedColumnName = "RENTAL_ID")
    private Rental rental;

    @ManyToOne
    @JoinColumn(name = "ROOM_ID", referencedColumnName = "ROOM_ID")
    private Room room;

    @OneToMany(mappedBy = "rentalDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceUsage> serviceUsages = new ArrayList<>();

    @OneToMany(mappedBy = "rentalDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Incident> incidents = new ArrayList<>();
}