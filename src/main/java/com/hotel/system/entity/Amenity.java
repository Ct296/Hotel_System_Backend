package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "AMENITY")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Amenity {
    @Id
    @Column(name = "AMENITY_ID", length = 10)
    private String id;

    @Column(name = "AMENITY_Name", nullable = false, length = 100)
    private String name;
}