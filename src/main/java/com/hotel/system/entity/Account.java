package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ACCOUNT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @Column(name = "USER_ID", length = 10)
    private String id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "USER_ID")
    private Users user;

    @Column(name = "USER_Password", nullable = false, length = 255)
    private String password;
}