package com.hotel.system.dto.admin;

import com.hotel.system.entity.enums.AccountState;
import com.hotel.system.entity.enums.Role;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminAccountView {
    private String userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String pid;
    private String nationality;
    private String avatar;
    private Role role;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

    private AccountState currentStatus;
    private String currentStatusReason;
    private LocalDateTime currentStatusStartTime;
    private LocalDateTime currentStatusEndTime;
}