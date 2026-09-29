package com.hotel.system.entity;

import com.hotel.system.entity.enums.ManagerType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "MANAGER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Manager {

    @Id
    @Column(name = "USER_ID", length = 10)
    private String id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "USER_ID")
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "MANAGER_JobTitle", nullable = false, length = 30)
    private ManagerType title;

}