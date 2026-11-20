package com.hotel.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "INCIDENT_Name", length = 255, nullable = false)
    private String name;

    @Column(name = "INCIDENT_OccurTime", nullable = false)
    private LocalDateTime occurTime;

    @Column(name = "INCIDENT_Description", length = 255)
    private String description;

    @Column(name = "INCIDENT_Responsible", length = 80, nullable = false)
    private String responsible;

    @Column(name = "INCIDENT_Money", nullable = false)
    private Double money;

    @Column(name = "INCIDENT_Handle", length = 255)
    private String handle;

    @Enumerated(EnumType.STRING)
    @Column(name = "INCIDENT_Status", length = 21, nullable = false)
    private IncidentStatus status;

    @ManyToOne(optional = false)
    @JsonIgnore
    @JoinColumn(name = "RENTAL_DETAIL_ID", nullable = false)
    private RentalDetail rentalDetail;

    @ManyToOne
    @JoinColumn(name = "CREATED_BY", nullable = false)
    private Users createdBy;
}