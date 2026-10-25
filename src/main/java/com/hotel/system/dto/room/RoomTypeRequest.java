package com.hotel.system.dto.room;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RoomTypeRequest {
    private String name;
    private String bed;
    private Integer adultMax;
    private Integer childrenMax;
    private BigDecimal area;
    private Double basePrice;
    private Double depositPercent;
    private String description;
    private List<String> amenityIds; // Dành cho tính năng gán tiện ích sau này
}