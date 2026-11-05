package com.hotel.system.controller;

import com.hotel.system.dto.incident.IncidentCreateRequest;
import com.hotel.system.entity.Incident;
import com.hotel.system.entity.Manager;
import com.hotel.system.entity.RentalDetail;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.repository.IncidentRepository;
import com.hotel.system.repository.ManagerRepository;
import com.hotel.system.repository.RentalDetailRepository;
import com.hotel.system.repository.UsersRepository;
import com.hotel.system.service.IncidentService;
import com.hotel.system.util.AppUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/operation")
@RequiredArgsConstructor
public class IncidentApiController {

    private final IncidentService incidentService;
    private final IncidentRepository incidentRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final UsersRepository usersRepository;
    private final ManagerRepository managerRepository;

    // Giữ nguyên logic API gốc của Staff
    @PostMapping("/api/staff/incidents/create")
    public ResponseEntity<?> createIncident(@RequestBody IncidentCreateRequest request, Principal principal) {
        try {
            incidentService.createIncident(request, principal.getName());
            return ResponseEntity.ok().body(Map.of("message", "Báo cáo sự cố thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    private boolean hasOperationAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) return true;
        if (auth.getPrincipal() instanceof UserDetails userDetails) {
            Users user = usersRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (user != null && user.getRole() == Role.MANAGER) {
                Manager manager = managerRepository.findById(user.getId()).orElse(null);
                return manager != null && manager.getTitle().name().equals("OPERATION_MANAGER");
            }
        }
        return false;
    }

    @GetMapping("/incidents")
    public ResponseEntity<?> getAllIncidents() {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        
        List<Incident> incidents = incidentRepository.findAllByOrderByOccurTimeDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Incident i : incidents) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", i.getId());
            map.put("name", i.getName());
            map.put("occurTime", i.getOccurTime());
            map.put("description", i.getDescription());
            map.put("responsible", i.getResponsible());
            map.put("money", i.getMoney());
            map.put("handle", i.getHandle());
            map.put("status", i.getStatus());
            
            if (i.getRentalDetail() != null) {
                map.put("rentalDetailId", i.getRentalDetail().getId());
                if (i.getRentalDetail().getRoom() != null) {
                    map.put("roomName", i.getRentalDetail().getRoom().getName()); 
                }
            }
            result.add(map);
        }
        
        List<RentalDetail> activeRentals = rentalDetailRepository.findAll().stream()
                .filter(rd -> rd.getStatus() == null || 
                              "IN_PROGRESS".equals(rd.getStatus().name()) || 
                              "CHECKED_IN".equals(rd.getStatus().name())) 
                .toList();
        
        List<Map<String, Object>> rentals = new ArrayList<>();
        for (RentalDetail rd : activeRentals) {
            if(rd.getRoom() != null) {
                rentals.add(Map.of("id", rd.getId(), "roomName", rd.getRoom().getName()));
            }
        }
        
        return ResponseEntity.ok(Map.of("incidents", result, "activeRentals", rentals));
    }

    @PostMapping("/incidents")
    @Transactional
    public ResponseEntity<?> saveIncident(@RequestBody Map<String, Object> payload) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        
        String id = (String) payload.get("id");
        boolean isCreate = id == null || id.isBlank();
        Incident incident = isCreate ? new Incident() : incidentRepository.findById(id).orElse(null);
        
        if (incident == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy sự cố"));

        if (!isCreate && incident.getStatus() != null && "RESOLVED".equals(incident.getStatus().toString())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể sửa phiếu sự cố đã giải quyết xong để đảm bảo lưu vết."));
        }

        if (isCreate) {
            incident.setId(AppUtils.generateId("ICD", 10));
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth.getPrincipal() instanceof UserDetails userDetails) {
                usersRepository.findByEmail(userDetails.getUsername()).ifPresent(incident::setCreatedBy);
            }
        }

        incident.setName((String) payload.get("name"));
        incident.setDescription((String) payload.get("description"));
        incident.setOccurTime(LocalDateTime.parse(payload.get("occurTime").toString()));
        incident.setResponsible((String) payload.get("responsible"));
        
        Object moneyObj = payload.get("money");
        incident.setMoney(moneyObj != null && !moneyObj.toString().isBlank() ? Double.valueOf(moneyObj.toString()) : 0.0);
        
        incident.setHandle((String) payload.get("handle"));
        
        incident.setStatus(com.hotel.system.entity.enums.IncidentStatus.valueOf((String) payload.get("status"))); 
        
        String rentalDetailId = (String) payload.get("rentalDetailId");
        if (rentalDetailId != null && !rentalDetailId.isBlank()) {
            rentalDetailRepository.findById(rentalDetailId).ifPresent(incident::setRentalDetail);
        }

        incidentRepository.save(incident);
        return ResponseEntity.ok(Map.of("message", isCreate ? "Tạo sự cố thành công!" : "Cập nhật sự cố thành công!"));
    }
}
