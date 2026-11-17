package com.hotel.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "RENTAL_GUEST")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalGuest {
    @Id
    @Column(name = "RENTAL_GUEST_ID", length = 10)
    private String id;

    @Column(name = "RENTAL_GUEST_FullName", nullable = false, length = 60)
    private String fullName;

    @Column(name = "RENTAL_GUEST_PID", length = 20)
    private String pid; // CCCD/Hộ chiếu (có thể trống nếu là trẻ em)

    @Column(name = "RENTAL_GUEST_Phone", length = 15)
    private String phoneNumber;

    @Column(name = "RENTAL_GUEST_IsAdult", nullable = false)
    private Boolean isAdult = true; // true: Người lớn, false: Trẻ em

    @Column(name = "RENTAL_GUEST_IsRepresentative", nullable = false)
    private Boolean isRepresentative = false; // Đánh dấu người đại diện phòng

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "RENTAL_DETAIL_ID", nullable = false)
    private RentalDetail rentalDetail;
}