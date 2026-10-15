package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.hotel.system.entity.enums.WorkingStatus;

@Entity
@Table(name = "STAFF")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Staff {

    @Id
    @Column(name = "USER_ID", length = 10)
    private String id;

    @Column(name = "STAFF_EmploymentTime", nullable = false)
    private LocalDateTime employmentTime;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "USER_ID")
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "STAFF_WorkingStatus", nullable = false)
    private WorkingStatus workingStatus = WorkingStatus.WORKING;
}