package com.hotel.system.entity;

import com.hotel.system.entity.enums.IncidentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "INCIDENT")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Incident {
    @Id
    @Column(name = "INCIDENT_ID", length = 10)
    private String id;

    @Column(name = "INCIDENT_Name", nullable = false, length = 100)
    private String name;

    @Column(name = "INCIDENT_OccurTime", nullable = false)
    private LocalDateTime occurTime;

    @Column(name = "INCIDENT_Description", length = 255)
    private String description;

    @Column(name = "INCIDENT_Responsible", length = 50)
    private String responsible;

    @Column(name = "INCIDENT_Money")
    private Double money;

    @Column(name = "INCIDENT_Handle", length = 255)
    private String handle;

    @Enumerated(EnumType.STRING)
    @Column(name = "INCIDENT_Status", nullable = false, length = 20)
    private IncidentStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "RENTAL_DETAIL_ID", nullable = false)
    private RentalDetail rentalDetail;
}