package com.hotel.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ROOM_IMAGE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomImage {

    @Id
    @Column(name = "ROOM_IMAGE_ID", length = 10)
    private String id;

    @Column(name = "ROOM_IMAGE_Path", nullable = false, length = 255)
    private String imagePath;

    @Column(name = "ROOM_IMAGE_IsPrimary", nullable = false)
    private Boolean isPrimary = false;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "ROOM_ID", nullable = false)
    private Room room;
}