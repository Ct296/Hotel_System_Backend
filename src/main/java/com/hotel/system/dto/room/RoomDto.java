package com.hotel.system.dto.room;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RoomDto {
    private String id;
    private String name;
    private String location;
    private String status;
    
    // Thông tin từ RoomType
    private String typeName;
    private String bedType;
    private Integer adultMax;
    private Integer childrenMax;
    private Double basePrice;
    
    // Ảnh đại diện (isPrimary = true)
    private String primaryImage;
}