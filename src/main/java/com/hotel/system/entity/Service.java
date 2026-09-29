package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "SERVICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    @Id
    @Column(name = "SERVICE_ID", length = 10)
    private String id;

    @Column(name = "SERVICE_Name", nullable = false, length = 50)
    private String name;

    @Column(name = "SERVICE_Description", length = 255)
    private String description;

    @Column(name = "SERVICE_Unit", nullable = false, length = 10)
    private String unit;

    @Column(name = "SERVICE_ImagePath", length = 255)
    private String imagePath;

    @Column(name = "SERVICE_CreateDate", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "SERVICE_UpdateDate")
    private LocalDateTime updateDate;

    @Column(name = "SERVICE_BasePrice", nullable = false)
    private Double basePrice;


}