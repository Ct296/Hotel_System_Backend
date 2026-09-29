package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "ADMIN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Admin {
    @Id
    @Column(name = "USER_ID")
    private String id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "USER_ID")
    private Users user;
}