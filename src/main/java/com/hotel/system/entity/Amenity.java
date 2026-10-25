package com.hotel.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
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

    @OneToMany(mappedBy = "amenity")
    @JsonIgnore 
    private List<RoomAmenity> roomAmenities;
}