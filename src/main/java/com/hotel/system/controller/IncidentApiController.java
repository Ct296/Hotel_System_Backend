package com.hotel.system.controller;

import com.hotel.system.dto.incident.IncidentCreateRequest;
import com.hotel.system.dto.incident.IncidentUpdateRequest;
import com.hotel.system.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class IncidentApiController {

    private final IncidentService incidentService;

    // API cho Lễ tân tạo sự cố (Được ánh xạ vào /api/staff/..)
    @PostMapping("/api/staff/incidents/create")
    public ResponseEntity<?> createIncident(@RequestBody IncidentCreateRequest request, Principal principal) {
        try {
            // principal.getName() sẽ tự động lấy được email từ JWT Token nhờ SecurityContextHolder
            incidentService.createIncident(request, principal.getName());
            return ResponseEntity.ok().body(Map.of("message", "Báo cáo sự cố thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    // API cho Manager duyệt/cập nhật sự cố (Được ánh xạ vào /api/manager/..)
    @PutMapping("/api/manager/incidents/update/{id}")
    public ResponseEntity<?> updateIncident(@PathVariable String id, @RequestBody IncidentUpdateRequest request) {
        try {
            incidentService.updateIncident(id, request);
            return ResponseEntity.ok().body(Map.of("message", "Cập nhật sự cố thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }
}