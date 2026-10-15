package com.hotel.system.entity;

import com.hotel.system.entity.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ROOM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @Column(name = "ROOM_ID", length = 10)
    private String id;

    @Column(name = "ROOM_Name", nullable = false, length = 30)
    private String name;

    @Column(name = "ROOM_Location", nullable = false, length = 20)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROOM_Status", nullable = false, length = 30)
    private RoomStatus status;

    @ManyToOne
    @JoinColumn(name = "ROOM_TYPE_ID", nullable = false)
    private RoomType roomType;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomImage> images = new ArrayList<>();
}
