package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "REVIEW",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_REVIEW_USER", columnNames = "USER_ID")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @Column(name = "REVIEW_ID", length = 10)
    private String id;

    @Column(name = "REVIEW_Rate", nullable = false)
    private Integer rate;

    @Column(name = "REVIEW_Description", length = 255)
    private String description;

    @Column(name = "REVIEW_UpdateDate", nullable = false)
    private LocalDateTime updateDate;

    @OneToOne(optional = false)
    @JoinColumn(name = "USER_ID", nullable = false, unique = true)
    private Customer customer;
}
