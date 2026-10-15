package com.hotel.system.entity;

import com.hotel.system.entity.enums.IncidentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "INCIDENT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @Column(name = "INCIDENT_ID", length = 10)
    private String id;

    @Column(name = "INCIDENT_Name")
    private String name;

    @Column(name = "INCIDENT_OccurTime")
    private LocalDateTime occurTime;

    @Column(name = "INCIDENT_Description", length = 255)
    private String description;

    @Column(name = "INCIDENT_Responsible", length = 20)
    private String responsible;

    @Column(name = "INCIDENT_Money")
    private Double money;

    @Column(name = "INCIDENT_Handle", length = 20)
    private String handle;

    @Enumerated(EnumType.STRING)
    @Column(name = "INCIDENT_Status", length = 21)
    private IncidentStatus status;

    @ManyToOne
    @JoinColumn(name = "RENTAL_DETAIL_ID")
    private RentalDetail rentalDetail;
}