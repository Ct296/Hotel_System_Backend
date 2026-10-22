package com.hotel.system.dto.incident;

import com.hotel.system.entity.enums.IncidentStatus;
import lombok.Data;

@Data
public class IncidentUpdateRequest {
    private String responsible;
    private Double money;
    private String handle;
    private IncidentStatus status;
}