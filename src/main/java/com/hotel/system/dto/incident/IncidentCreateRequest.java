package com.hotel.system.dto.incident;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IncidentCreateRequest {
    private String name;
    private LocalDateTime occurTime;
    private String description;
    private String rentalDetailId;
}