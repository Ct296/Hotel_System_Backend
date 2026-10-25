package com.hotel.system.controller;

import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.entity.enums.RoomStatus;
import com.hotel.system.entity.enums.ServiceStatus;
import com.hotel.system.repository.*;
import com.hotel.system.service.ManagerOperationService;
import com.hotel.system.service.MediaStorageService;
import com.hotel.system.util.AppUtils;
import com.hotel.system.util.StoredMedia;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.hotel.system.dto.room.RoomTypeRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/manager/operation")
@RequiredArgsConstructor
public class ManagerOperationController {
    
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final PriceRateRepository priceRateRepository;
    private final AppliedPeriodRepository appliedPeriodRepository;
    private final AmenityRepository amenityRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceUsageRepository serviceUsageRepository;
    private final UsersRepository usersRepository;
    private final ManagerRepository managerRepository;
    private final MediaStorageService mediaStorageService;
    private final ManagerOperationService managerOperationService;
    
    // Inject Repository mới tạo
    private final RoomAmenityRepository roomAmenityRepository;

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

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));

        List<RoomType> roomTypes = roomTypeRepository.findAllByOrderByNameAsc();
        List<Room> rooms = roomRepository.findAllByOrderByNameAsc();
        List<PriceRate> priceRates = priceRateRepository.findAllByOrderByCreateDateDesc();
        List<AppliedPeriod> appliedPeriods = appliedPeriodRepository.findAllByOrderByStartDateDesc();
        List<Amenity> allAmenities = amenityRepository.findAll();
        List<Service> services = serviceRepository.findAllByOrderByCreateDateDesc();
        List<ServiceUsage> serviceUsages = serviceUsageRepository.findAllByOrderByTimeDesc();

        Map<String, Object> response = new HashMap<>();
        response.put("allAmenities", allAmenities);
        response.put("roomTypes", roomTypes);
        response.put("rooms", rooms);
        response.put("priceRates", priceRates);
        response.put("appliedPeriods", appliedPeriods);
        response.put("roomStatuses", RoomStatus.values());
        
        response.put("roomCountByType", managerOperationService.buildRoomCountByType(roomTypes, rooms));
        response.put("roomStatusByType", managerOperationService.buildRoomStatusByType(roomTypes, rooms));
        response.put("roomTypeCount", roomTypes.size());
        response.put("roomCount", rooms.size());
        response.put("priceRateCount", priceRates.size());
        response.put("activeAppliedPeriodCount", appliedPeriodRepository.countByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDateTime.now(), LocalDateTime.now()));

        response.put("services", services);
        response.put("serviceUsages", serviceUsages);
        response.put("serviceStatuses", ServiceStatus.values());
        response.put("serviceCount", services.size());
        response.put("activeServiceCount", services.stream().filter(item -> item.getStatus() == ServiceStatus.ACTIVE).count());
        response.put("suspendedServiceCount", services.stream().filter(item -> item.getStatus() == ServiceStatus.SUSPENDED).count());
        response.put("serviceUsageCount", serviceUsages.size());
        response.put("popularServiceSummary", managerOperationService.buildPopularServiceSummary(services, serviceUsages));
        response.put("revenueByService", managerOperationService.buildRevenueByService(services, serviceUsages));

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // API QUẢN LÝ LOẠI PHÒNG (CHUẨN RESTful)
    // ==========================================

    @GetMapping("/room-types")
    public ResponseEntity<?> getAllRoomTypes() {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
        return ResponseEntity.ok(roomTypeRepository.findAllByOrderByNameAsc());
    }

    @PostMapping("/room-types")
    @Transactional
    public ResponseEntity<?> createRoomType(@RequestBody RoomTypeRequest request) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
        
        String trimmedName = request.getName() == null ? "" : request.getName().trim();
        if (trimmedName.isBlank() || request.getAdultMax() < 1 || request.getChildrenMax() < 0 || 
            request.getArea().compareTo(BigDecimal.ZERO) <= 0 || request.getBasePrice() < 0 || 
            request.getDepositPercent() < 0 || request.getDepositPercent() > 100) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dữ liệu không hợp lệ."));
        }
        
        if (roomTypeRepository.findByName(trimmedName) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tên loại phòng đã tồn tại."));
        }

        RoomType roomType = new RoomType();
        roomType.setId(AppUtils.generateId("RTY", 10));
        roomType.setCreateDate(LocalDateTime.now());
        roomType.setUpdateDate(LocalDateTime.now());
        
        mapRoomTypeData(roomType, request, trimmedName);
        
        // Bắt buộc lưu RoomType trước để có dữ liệu trong DB
        roomTypeRepository.save(roomType);
        
        // --- Tự tay lưu Tiện ích ---
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            amenityRepository.findAllById(request.getAmenityIds()).forEach(amenity -> {
                RoomAmenity ra = new RoomAmenity();
                ra.setRoomType(roomType);
                ra.setAmenity(amenity);
                roomAmenityRepository.save(ra);
            });
        }
        
        return ResponseEntity.ok(Map.of("message", "Thêm loại phòng mới thành công."));
    }

    @PutMapping("/room-types/{id}")
    @Transactional
    public ResponseEntity<?> updateRoomType(@PathVariable String id, @RequestBody RoomTypeRequest request) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
        
        RoomType roomType = roomTypeRepository.findById(id).orElse(null);
        if (roomType == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy loại phòng."));

        String trimmedName = request.getName() == null ? "" : request.getName().trim();
        RoomType existingByName = roomTypeRepository.findByName(trimmedName);
        if (existingByName != null && !existingByName.getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tên loại phòng đã tồn tại."));
        }

        roomType.setUpdateDate(LocalDateTime.now());
        mapRoomTypeData(roomType, request, trimmedName);
        
        // Bắt buộc lưu RoomType trước
        roomTypeRepository.save(roomType);
        
        // --- Xử lý Cập nhật Tiện ích bằng tay ---
        List<String> selectedIds = request.getAmenityIds() != null ? request.getAmenityIds() : new ArrayList<>();
        
        // Lấy danh sách tiện ích cũ trong DB
        List<RoomAmenity> currentAmenities = roomAmenityRepository.findByRoomType(roomType);
        List<String> currentIds = currentAmenities.stream().map(ra -> ra.getAmenity().getId()).toList();
        
        // Xóa những tiện ích bị người dùng bỏ tick
        List<RoomAmenity> toDelete = currentAmenities.stream()
            .filter(ra -> !selectedIds.contains(ra.getAmenity().getId()))
            .toList();
        roomAmenityRepository.deleteAll(toDelete);
        
        // Thêm những tiện ích người dùng mới tick vào
        List<String> newIds = selectedIds.stream().filter(aid -> !currentIds.contains(aid)).toList();
        if (!newIds.isEmpty()) {
            amenityRepository.findAllById(newIds).forEach(amenity -> {
                RoomAmenity ra = new RoomAmenity();
                ra.setRoomType(roomType);
                ra.setAmenity(amenity);
                roomAmenityRepository.save(ra);
            });
        }
        
        return ResponseEntity.ok(Map.of("message", "Cập nhật loại phòng thành công."));
    }

    @DeleteMapping("/room-types/{id}")
    @Transactional
    public ResponseEntity<?> deleteRoomType(@PathVariable String id) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
        try {
            roomTypeRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa loại phòng."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể xóa loại phòng đang có phòng liên kết."));
        }
    }

    // Hàm hỗ trợ map dữ liệu
    private void mapRoomTypeData(RoomType roomType, RoomTypeRequest request, String trimmedName) {
        roomType.setName(trimmedName);
        roomType.setAdultMax(request.getAdultMax());
        roomType.setChildrenMax(request.getChildrenMax());
        roomType.setBed(request.getBed() == null ? "" : request.getBed().trim());
        roomType.setArea(request.getArea());
        roomType.setBasePrice(request.getBasePrice());
        roomType.setDepositPercent(request.getDepositPercent());
        roomType.setDescription(request.getDescription() == null || request.getDescription().isBlank() ? null : request.getDescription().trim());
    }

    // ==========================================
    // CÁC API KHÁC GIỮ NGUYÊN
    // ==========================================

    @PostMapping("/rooms")
    @Transactional
    public ResponseEntity<?> saveRoom(@RequestParam(required = false) String roomId, @RequestParam String name,
                           @RequestParam String location, @RequestParam String roomTypeId,
                           @RequestParam RoomStatus status,
                           @RequestParam(value = "primaryImageFile", required = false) MultipartFile primaryImageFile,
                           @RequestParam(value = "secondaryImageFiles", required = false) List<MultipartFile> secondaryImageFiles) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        
        String trimmedName = name == null ? "" : name.trim();
        String trimmedLocation = location == null ? "" : location.trim();
        RoomType roomType = roomTypeRepository.findById(roomTypeId).orElse(null);
        
        if (trimmedName.isBlank() || trimmedLocation.isBlank() || status == null || roomType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dữ liệu không hợp lệ."));
        }

        boolean isCreate = roomId == null || roomId.isBlank();
        Room room = isCreate ? new Room() : roomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy phòng."));

        if (isCreate) {
            room.setId(AppUtils.generateId("ROM", 10));
            room.setStatus(status);
        }
        room.setName(trimmedName);
        room.setLocation(trimmedLocation);
        room.setRoomType(roomType);
        room.setStatus(status);

        boolean hasPrimaryUpload = primaryImageFile != null && !primaryImageFile.isEmpty();
        if (isCreate && !hasPrimaryUpload) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phòng mới bắt buộc phải có ảnh chính."));
        }

        room = roomRepository.save(room);
        try {
            managerOperationService.processRoomImages(room, isCreate, primaryImageFile, secondaryImageFiles);
            return ResponseEntity.ok(Map.of("message", isCreate ? "Thêm phòng mới thành công." : "Cập nhật phòng thành công."));
        } catch (Exception e) {
            if (isCreate) roomRepository.delete(room);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/rooms/{roomId}/images/{imageId}")
    @Transactional
    public ResponseEntity<?> deleteRoomImage(@PathVariable String roomId, @PathVariable String imageId) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Room room = roomRepository.findById(roomId).orElse(null);
        RoomImage roomImage = roomImageRepository.findById(imageId).orElse(null);
        
        if (room == null || roomImage == null || !roomId.equals(roomImage.getRoom().getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy ảnh."));
        }
        try {
            managerOperationService.deleteRoomImage(room, roomImage);
            return ResponseEntity.ok(Map.of("message", "Xóa ảnh thành công."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/price-rates")
    @Transactional
    public ResponseEntity<?> savePriceRate(@RequestBody Map<String, Object> payload) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        String priceRateId = (String) payload.get("priceRateId");
        String eventName = (String) payload.get("eventName");
        Double surchargeAmount = Double.valueOf(payload.get("surchargeAmount").toString());

        String trimmedEventName = eventName == null ? "" : eventName.trim();
        // Kiểm tra hợp lệ: Tên không rỗng và % phải nằm trong khoảng -100 đến 100
        if (trimmedEventName.isBlank() || surchargeAmount == null || surchargeAmount < -100 || surchargeAmount > 100) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dữ liệu không hợp lệ. Mức % phải từ -100 đến 100."));
        }
        
        boolean isCreate = priceRateId == null || priceRateId.isBlank();
        PriceRate priceRate = isCreate ? new PriceRate() : priceRateRepository.findById(priceRateId).orElse(null);
        
        if (priceRate == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy chương trình giá."));

        if (isCreate) {
            priceRate.setId(AppUtils.generateId("PRC", 10));
            priceRate.setCreateDate(LocalDateTime.now());
        }
        priceRate.setUpdateDate(LocalDateTime.now());
        priceRate.setEventName(trimmedEventName);
        priceRate.setSurchargeAmount(surchargeAmount);
        
        priceRateRepository.save(priceRate);
        return ResponseEntity.ok(Map.of("message", isCreate ? "Tạo chương trình giá thành công." : "Cập nhật chương trình giá thành công."));
    }

    @DeleteMapping("/price-rates/{id}")
    public ResponseEntity<?> deletePriceRate(@PathVariable String id) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            priceRateRepository.deleteById(id);
            priceRateRepository.flush();      
            return ResponseEntity.ok(Map.of("message", "Đã xóa chương trình giá thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể xóa vì chương trình này đang được gắn vào một Kỳ áp dụng. Vui lòng sang Tab 2 gỡ kỳ áp dụng trước!"));
        }
    }

    @PostMapping("/applied-periods")
    @Transactional
    public ResponseEntity<?> saveAppliedPeriod(@RequestBody Map<String, Object> payload) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        String appliedPeriodId = (String) payload.get("appliedPeriodId");
        if (appliedPeriodId != null && !appliedPeriodId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Kỳ áp dụng bị khóa chỉnh sửa."));
        }
        try {
            LocalDateTime start = LocalDateTime.parse(payload.get("startDate").toString());
            LocalDateTime end = LocalDateTime.parse(payload.get("endDate").toString());
            if (!end.isAfter(start)) throw new IllegalArgumentException();
            
            AppliedPeriod ap = new AppliedPeriod();
            ap.setId(AppUtils.generateId("APD", 10));
            ap.setPriceRate(priceRateRepository.findById(payload.get("priceRateId").toString()).orElseThrow());
            ap.setRoomType(roomTypeRepository.findById(payload.get("roomTypeId").toString()).orElseThrow());
            ap.setStartDate(start);
            ap.setEndDate(end);
            appliedPeriodRepository.save(ap);
            return ResponseEntity.ok(Map.of("message", "Thêm kỳ áp dụng thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi lưu kỳ áp dụng."));
        }
    }



    @DeleteMapping("/applied-periods/{id}")
    @Transactional
    public ResponseEntity<?> deleteAppliedPeriod(@PathVariable String id) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        appliedPeriodRepository.findById(id).ifPresent(appliedPeriodRepository::delete);
        return ResponseEntity.ok(Map.of("message", "Xóa kỳ áp dụng thành công."));
    }

    @PostMapping("/services")
    @Transactional
    public ResponseEntity<?> saveService(@RequestParam(required = false) String serviceId, @RequestParam String name,
                              @RequestParam(required = false) String description, @RequestParam String unit,
                              @RequestParam Double basePrice, @RequestParam(required = false) MultipartFile imageFile,
                              @RequestParam ServiceStatus status) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        String trimmedName = name == null ? "" : name.trim();
        String trimmedUnit = unit == null ? "" : unit.trim();
        if (trimmedName.isBlank() || trimmedUnit.isBlank() || basePrice == null || basePrice < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dữ liệu không hợp lệ."));
        }

        boolean isCreate = serviceId == null || serviceId.isBlank();
        Service service = isCreate ? new Service() : serviceRepository.findById(serviceId).orElse(null);
        if (service == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy dịch vụ."));

        if (isCreate) {
            service.setId(AppUtils.generateId("SER", 10));
            service.setCreateDate(LocalDateTime.now());
        }
        service.setName(trimmedName);
        service.setDescription(description == null || description.trim().isBlank() ? null : description.trim());
        service.setUnit(trimmedUnit);
        service.setBasePrice(basePrice);
        service.setStatus(status);
        service.setUpdateDate(LocalDateTime.now());

        if (imageFile != null && !imageFile.isEmpty()) {
            String oldPath = service.getImagePath();
            StoredMedia media = mediaStorageService.storeImage(imageFile, MediaStorageDirectory.SERVICES, service.getId());
            service.setImagePath(media.publicPath());
            if (oldPath != null && !oldPath.equals(media.publicPath())) mediaStorageService.deleteByPublicPath(oldPath);
        }

        serviceRepository.save(service);
        return ResponseEntity.ok(Map.of("message", isCreate ? "Thêm mới dịch vụ thành công." : "Cập nhật dịch vụ thành công."));
    }

    @DeleteMapping("/services/{id}")
    @Transactional
    public ResponseEntity<?> deleteService(@PathVariable String id) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Service service = serviceRepository.findById(id).orElse(null);
        if (service == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy dịch vụ."));
        
        if (serviceUsageRepository.countByServiceId(id) > 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể xóa dịch vụ đã có người dùng."));
        }
        
        mediaStorageService.deleteByPublicPath(service.getImagePath());
        serviceRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa dịch vụ."));
    }

    @PostMapping("/amenities")
    @Transactional
    public ResponseEntity<?> saveAmenity(@RequestBody Map<String, String> payload) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        String amenityId = payload.get("amenityId");
        String name = payload.get("name");
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tên tiện ích không được để trống."));
        }

        boolean isCreate = amenityId == null || amenityId.isBlank();
        Amenity amenity = isCreate ? new Amenity() : amenityRepository.findById(amenityId).orElse(null);
        if (amenity == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy tiện ích."));

        if (isCreate) amenity.setId(AppUtils.generateId("AME", 10));
        amenity.setName(trimmedName);
        amenityRepository.save(amenity);

        return ResponseEntity.ok(Map.of("message", isCreate ? "Thêm mới tiện ích thành công." : "Cập nhật tiện ích thành công."));
    }

    @DeleteMapping("/amenities/{id}")
    @Transactional
    public ResponseEntity<?> deleteAmenity(@PathVariable String id) {
        if (!hasOperationAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            amenityRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa tiện ích thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể xóa tiện ích đang được gán cho phòng."));
        }
    }
}