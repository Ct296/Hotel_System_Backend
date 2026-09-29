package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "WORK_ASSIGNMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkAssignment {

    @Id
    @Column(name = "WORK_ASSIGNMENT_ID", length = 10)
    private String id;

    @Column(name = "WORK_ASSIGNMENT_AssignedAt", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "WORK_ASSIGNMENT_EndAt")
    private LocalDateTime endAt;

    @Column(name = "WORK_ASSIGNMENT_Note", length = 255)
    private String note;

    @ManyToOne(optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private Staff staff;

    @ManyToOne(optional = false)
    @JoinColumn(name = "WORK_SCHEDULE_ID", nullable = false)
    private WorkSchedule workSchedule;
}