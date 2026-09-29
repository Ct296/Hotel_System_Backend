package com.hotel.system.entity;

import com.hotel.system.entity.enums.AccountState;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT_STATUS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatus {

    @Id
    @Column(name = "ACCOUNT_STATUS_ID", length = 10)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACCOUNT_STATUS_Name", nullable = false, length = 20)
    private AccountState name;

    @Column(name = "ACCOUNT_STATUS_StartTime", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "ACCOUNT_STATUS_EndTime")
    private LocalDateTime endTime;

    @Column(name = "ACCOUNT_STATUS_Reason", length = 255)
    private String reason;

    @ManyToOne(optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private Account account;
}