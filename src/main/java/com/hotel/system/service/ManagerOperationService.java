package com.hotel.system.service;

import com.hotel.system.entity.*;
import com.hotel.system.repository.MediaStorageDirectory;
import com.hotel.system.repository.RoomImageRepository;
import com.hotel.system.util.AppUtils;
import com.hotel.system.util.StoredMedia;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerOperationService {

    private final RoomImageRepository roomImageRepository;
    private final MediaStorageService mediaStorageService;

    // 1. TÍNH TOÁN DỮ LIỆU DASHBOARD

    public Map<String, Long> buildRoomCountByType(List<RoomType> types, List<Room> rooms) {
        return types.stream().collect(Collectors.toMap(RoomType::getName,
                t -> rooms.stream().filter(r -> r.getRoomType() != null && r.getRoomType().getId().equals(t.getId())).count(),
                (a, b) -> a, LinkedHashMap::new));
    }

    public Map<String, String> buildRoomStatusByType(List<RoomType> types, List<Room> rooms) {
        Map<String, String> result = new LinkedHashMap<>();
        for (RoomType t : types) {
            Map<String, Long> grp = rooms.stream()
                    .filter(r -> r.getRoomType() != null && t.getId().equals(r.getRoomType().getId()) && r.getStatus() != null)
                    .collect(Collectors.groupingBy(r -> r.getStatus().name(), LinkedHashMap::new, Collectors.counting()));
            result.put(t.getName(), grp.isEmpty() ? "Chưa có" : grp.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(" | ")));
        }
        return result;
    }

    public Map<String, Long> buildPopularServiceSummary(List<com.hotel.system.entity.Service> services, List<ServiceUsage> usages) {
        return services.stream().collect(Collectors.toMap(com.hotel.system.entity.Service::getName,
                s -> usages.stream()
                        .filter(u -> u.getService() != null && s.getId().equals(u.getService().getId()))
                        .mapToLong(u -> u.getCount() == null ? 0 : u.getCount()).sum(),
                (a, b) -> a, LinkedHashMap::new));
    }

    public Map<String, Double> buildRevenueByService(List<com.hotel.system.entity.Service> services, List<ServiceUsage> usages) {
        return services.stream().collect(Collectors.toMap(com.hotel.system.entity.Service::getName,
                s -> usages.stream()
                        .filter(u -> u.getService() != null && s.getId().equals(u.getService().getId()))
                        .mapToDouble(u -> (u.getCount() == null ? 0 : u.getCount()) * (s.getBasePrice() == null ? 0 : s.getBasePrice())).sum(),
                (a, b) -> a, LinkedHashMap::new));
    }

    // 2. XỬ LÝ ẢNH PHÒNG

    public RoomImage getPrimaryImage(Room room) {
        if (room == null) return null;
        return roomImageRepository.findFirstByRoomAndIsPrimaryTrue(room).orElse(
                roomImageRepository.findAllByRoomOrderByIdAsc(room).stream()
                        .filter(i -> i.getImagePath() != null).findFirst().orElse(null));
    }

    public List<RoomImage> getSecondaryImages(Room room) {
        if (room == null) return List.of();
        return roomImageRepository.findAllByRoomOrderByIdAsc(room).stream()
                .filter(i -> !Boolean.TRUE.equals(i.getIsPrimary())).toList();
    }

    @Transactional
    public void processRoomImages(Room room, boolean isCreate, MultipartFile primary, List<MultipartFile> secondaries) {
        boolean hasPrimaryUpload = primary != null && !primary.isEmpty();
        List<MultipartFile> normalizedSecondaryFiles = secondaries != null ?
                secondaries.stream().filter(f -> f != null && !f.isEmpty()).toList() : List.of();

        if (isCreate) {
            StoredMedia storedPrimary = mediaStorageService.storeImage(primary, MediaStorageDirectory.ROOMS, room.getId() + "_primary");
            saveRoomImage(room, storedPrimary.publicPath(), true);
            normalizedSecondaryFiles.forEach(f -> saveRoomImage(room, mediaStorageService.storeImage(f, MediaStorageDirectory.ROOMS, room.getId() + "_sec").publicPath(), false));
        } else {
            if (hasPrimaryUpload) {
                RoomImage existing = getPrimaryImage(room);
                StoredMedia stored = mediaStorageService.storeImage(primary, MediaStorageDirectory.ROOMS, room.getId() + "_primary");
                if (existing == null) {
                    saveRoomImage(room, stored.publicPath(), true);
                } else {
                    mediaStorageService.deleteByPublicPath(existing.getImagePath());
                    existing.setImagePath(stored.publicPath());
                    existing.setIsPrimary(true);
                    roomImageRepository.save(existing);
                }
            }
            normalizedSecondaryFiles.forEach(f -> saveRoomImage(room, mediaStorageService.storeImage(f, MediaStorageDirectory.ROOMS, room.getId() + "_sec").publicPath(), false));
        }
    }

    private void saveRoomImage(Room room, String path, boolean isPrimary) {
        RoomImage img = new RoomImage();
        img.setId(AppUtils.generateId("RIM", 10));
        img.setRoom(room);
        img.setImagePath(path);
        img.setIsPrimary(isPrimary);
        roomImageRepository.save(img);
    }

    @Transactional
    public void deleteRoomImage(Room room, RoomImage roomImage) {
        if (roomImageRepository.findAllByRoomOrderByIdAsc(room).size() <= 1) {
            throw new IllegalStateException("Không thể xóa ảnh cuối cùng của phòng.");
        }
        boolean deletingPrimary = Boolean.TRUE.equals(roomImage.getIsPrimary());
        roomImageRepository.delete(roomImage);
        mediaStorageService.deleteByPublicPath(roomImage.getImagePath());

        if (deletingPrimary) {
            roomImageRepository.findAllByRoomOrderByIdAsc(room).stream()
                    .filter(img -> !img.getId().equals(roomImage.getId()))
                    .findFirst().ifPresent(img -> {
                        img.setIsPrimary(true);
                        roomImageRepository.save(img);
                    });
        }
    }
}