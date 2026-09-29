package com.hotel.system.entity;

import com.hotel.system.entity.enums.Gender;
import com.hotel.system.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "USERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    @Id
    @Column(name = "USER_ID", length = 10)
    private String id;

    @Column(name = "USER_FirstName", nullable = false, length = 30)
    private String firstName;

    @Column(name = "USER_LastName", nullable = false, length = 30)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_Sex", nullable = false, length = 20)
    private Gender sex;

    @Column(name = "USER_DateOfBirth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "USER_PID", unique = true, nullable = false, length = 20)
    private String pid;

    @Column(name = "USER_Nationality", nullable = false, length = 100)
    private String nationality;

    @Column(name = "USER_Email", unique = true, nullable = false, length = 50)
    private String email;

    @Column(name = "USER_PhoneNumber", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "USER_Avatar", length = 255)
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_Role", nullable = false, length = 20)
    private Role role;

    @Column(name = "USER_CreateDate", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "USER_UpdateDate")
    private LocalDateTime updateDate;
}