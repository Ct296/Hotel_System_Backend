package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CUSTOMER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @Column(name = "USER_ID", length = 10)
    private String id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "USER_ID")
    private Users user;
}