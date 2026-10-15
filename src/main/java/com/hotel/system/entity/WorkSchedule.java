package com.hotel.system.entity;

import com.hotel.system.entity.enums.Shift;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "WORK_SCHEDULE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkSchedule {

    @Id
    @Column(name = "WORK_SCHEDULE_ID", length = 10)
    private String id;

    @Column(name = "WORK_SCHEDULE_Date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "WORK_SCHEDULE_Shift", nullable = false, length = 20)
    private Shift shift;
}