package com.hotel.system.entity;

import com.hotel.system.entity.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "HISTORY_WORK")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoryWork {

    @Id
    @Column(name = "HISTORY_WORK_ID", length = 10)
    private String id;

    @Column(name = "HISTORY_WORK_CheckinTime")
    private LocalDateTime checkinTime;

    @Column(name = "HISTORY_WORK_CheckoutTime")
    private LocalDateTime checkoutTime;

    @Column(name = "HISTORY_WORK_IsOverTime")
    private Boolean isOverTime = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "HISTORY_WORK_Status", nullable = false, length = 30)
    private AttendanceStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private Staff staff;

    @ManyToOne(optional = false)
    @JoinColumn(name = "WORK_SCHEDULE_ID", nullable = false)
    private WorkSchedule workSchedule;
}
