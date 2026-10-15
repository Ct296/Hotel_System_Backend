package com.hotel.system.controller.api;

import com.hotel.system.service.RoomApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicApiController {

    @Autowired
    private RoomApiService roomApiService;

    // API lấy danh sách phòng hiển thị lên trang chủ
    @GetMapping("/rooms")
    public ResponseEntity<?> getAllRooms() {
        return ResponseEntity.ok(roomApiService.getAllPublicRooms());
    }
}