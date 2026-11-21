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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class IncidentApiController {

    private final IncidentService incidentService;
    private final IncidentRepository incidentRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final UsersRepository usersRepository;
    private final ManagerRepository managerRepository;
    private final com.hotel.system.service.BillingService billingService;

    // 1. API DÀNH CHO LỄ TÂN (STAFF)
    @PostMapping("/api/staff/incidents/create")
    public ResponseEntity<?> createIncident(@RequestBody IncidentCreateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Lấy email từ token JWT (auth.getName())
            String email = auth.getName();
            incidentService.createIncident(request, email);
            return ResponseEntity.ok().body(Map.of("message", "Báo cáo sự cố thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/api/staff/incidents")
    public ResponseEntity<?> getStaffIncidents() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Incident> incidents = incidentRepository.findAllByOrderByOccurTimeDesc();
            List<Map<String, Object>>  result = new ArrayList<>();
            
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
            return ResponseEntity.ok(Map.of("incidents", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    // 2. CÁC API DÀNH CHO QUẢN LÝ (MANAGER)
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

    @GetMapping("/api/manager/operation/incidents")
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
                RentalDetail rd = i.getRentalDetail();
                map.put("rentalDetailId", rd.getId());
                map.put("checkinDate", rd.getCheckinDate());
                if (rd.getRoom() != null) {
                    map.put("roomName", rd.getRoom().getName());
                }
                if (rd.getRental() != null) {
                    map.put("rentalId", rd.getRental().getId());
                    if (rd.getRental().getCustomer() != null && rd.getRental().getCustomer().getUser() != null) {
                        Users u = rd.getRental().getCustomer().getUser();
                        map.put("customerName", AppUtils.buildFullName(u.getFirstName(), u.getLastName()));
                        map.put("phoneNumber", u.getPhoneNumber());
                    }
                }
            }
            result.add(map);
        }

        List<RentalDetail> activeRentals = rentalDetailRepository.findAll().stream()
            .filter(rd -> rd.getStatus() != null
                    && ("CHECKED_IN".equals(rd.getStatus().name()) || "OVERDUE".equals(rd.getStatus().name())))
            .filter(rd -> rd.getRental() != null && !billingService.hasSettlementBill(rd.getRental().getId()))
            .toList();

        List<Map<String, Object>> rentals = new ArrayList<>();
        for (RentalDetail rd : activeRentals) {
            if (rd.getRoom() != null) {
                Map<String, Object> rMap = new HashMap<>();
                rMap.put("id", rd.getId());
                rMap.put("roomName", rd.getRoom().getName());
                rMap.put("rentalId", rd.getRental().getId());
                rMap.put("checkinDate", rd.getCheckinDate());
                rMap.put("status", rd.getStatus().name());
                if (rd.getRental().getCustomer() != null && rd.getRental().getCustomer().getUser() != null) {
                    Users u = rd.getRental().getCustomer().getUser();
                    rMap.put("customerName", AppUtils.buildFullName(u.getFirstName(), u.getLastName()));
                    rMap.put("phoneNumber", u.getPhoneNumber());
                } else {
                    rMap.put("customerName", "Khách vãng lai");
                    rMap.put("phoneNumber", "");
                }
                rentals.add(rMap);
            }
        }

        return ResponseEntity.ok(Map.of("incidents", result, "activeRentals", rentals));
    }

    @PostMapping("/api/manager/operation/incidents")
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

        String name = payload.get("name") != null ? payload.get("name").toString().trim() : "";
        if (name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tên sự cố không được để trống."));
        }

        LocalDateTime occurTime;
        try {
            occurTime = LocalDateTime.parse(payload.get("occurTime").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thời gian xảy ra sự cố không hợp lệ."));
        }

        String responsible = payload.get("responsible") != null
            ? payload.get("responsible").toString().trim().toUpperCase() : "NONE";
        if (!responsible.equals("CUSTOMER") && !responsible.equals("HOTEL") && !responsible.equals("NONE")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bên chịu trách nhiệm chỉ nhận CUSTOMER, HOTEL hoặc NONE"));
        }

        double moneyVal;
        try {
            Object moneyObj = payload.get("money");
            moneyVal = (moneyObj != null && !moneyObj.toString().isBlank())
                    ? Double.parseDouble(moneyObj.toString())
                    : 0.0;
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Số tiền sự cố không hợp lệ."));
        }
        if (moneyVal < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Số tiền sự cố không được âm"));
        }

        com.hotel.system.entity.enums.IncidentStatus status;
        try {
            status = com.hotel.system.entity.enums.IncidentStatus.valueOf(payload.get("status").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Trạng thái sự cố không hợp lệ."));
        }

        String rentalDetailId = (String) payload.get("rentalDetailId");
        if (isCreate) {
            if (rentalDetailId == null || rentalDetailId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn phòng đang lưu trú xảy ra sự cố."));
            }
            RentalDetail rd = rentalDetailRepository.findById(rentalDetailId).orElse(null);
            if (rd == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy chi tiết thuê phòng."));
            }
            
            if (rd.getStatus() == null
                    || (!"CHECKED_IN".equals(rd.getStatus().name()) && !"OVERDUE".equals(rd.getStatus().name()))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ có thể tạo sự cố cho phòng đang lưu trú."));
            }
            if (rd.getRental() == null || billingService.hasSettlementBill(rd.getRental().getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Đơn thuê này đã tất toán, không thể tạo thêm sự cố."));
            }

            incident.setId(AppUtils.generateId("ICD", 10));
            incident.setRentalDetail(rd);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth.getPrincipal() instanceof UserDetails userDetails) {
                usersRepository.findByEmail(userDetails.getUsername()).ifPresent(incident::setCreatedBy);
            }
        } else {
            if (rentalDetailId != null && !rentalDetailId.isBlank()
                    && incident.getRentalDetail() != null
                    && !rentalDetailId.equals(incident.getRentalDetail().getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không được phép thay đổi phòng/đơn thuê của sự cố đã tạo."));
            }
        }

        incident.setName(name);
        incident.setDescription((String) payload.get("description"));
        incident.setOccurTime(occurTime);
        incident.setResponsible(responsible);
        incident.setMoney("NONE".equals(responsible) ? 0.0 : moneyVal);
        incident.setHandle((String) payload.get("handle"));
        incident.setStatus(status);

        incidentRepository.save(incident);
        return ResponseEntity.ok(Map.of("message", isCreate ? "Tạo sự cố thành công!" : "Cập nhật sự cố thành công!"));
    }
}