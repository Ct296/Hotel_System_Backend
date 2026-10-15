package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "ROOM_AMENITY")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@IdClass(RoomAmenity.RoomAmenityId.class)
public class RoomAmenity {

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "ROOM_TYPE_ID", nullable = false)
    private RoomType roomType;

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "AMENITY_ID", nullable = false)
    private Amenity amenity;

    @EqualsAndHashCode
    public static class RoomAmenityId implements Serializable {
        private String roomType; 
        private String amenity; 
    }
}