package com.hotel.system.service;

import com.hotel.system.dto.room.RoomDto;
import com.hotel.system.entity.Room;
import com.hotel.system.entity.RoomImage;
import com.hotel.system.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomApiService {

    @Autowired
    private RoomRepository roomRepository;

    public List<RoomDto> getAllPublicRooms() {
        List<Room> rooms = roomRepository.findAllByOrderByNameAsc();
        
        return rooms.stream().map(room -> {
            // Tìm ảnh chính (isPrimary = true)
            String imageUrl = room.getImages().stream()
                    .filter(RoomImage::getIsPrimary)
                    .findFirst()
                    .map(RoomImage::getImagePath)
                    .orElse("/image/default_room.jpg"); // Ảnh mặc định nếu không có

            return RoomDto.builder()
                    .id(room.getId())
                    .name(room.getName())
                    .location(room.getLocation())
                    .status(room.getStatus().getDisplayName())
                    .typeName(room.getRoomType().getName())
                    .bedType(room.getRoomType().getBed())
                    .adultMax(room.getRoomType().getAdultMax())
                    .childrenMax(room.getRoomType().getChildrenMax())
                    .basePrice(room.getRoomType().getBasePrice())
                    .primaryImage(imageUrl)
                    .build();
        }).collect(Collectors.toList());
    }
}