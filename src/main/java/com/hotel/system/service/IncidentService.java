package com.hotel.system.service;

import com.hotel.system.dto.incident.IncidentCreateRequest;
import com.hotel.system.dto.incident.IncidentUpdateRequest;
import com.hotel.system.entity.Incident;
import com.hotel.system.entity.RentalDetail;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.IncidentStatus;
import com.hotel.system.repository.IncidentRepository;
import com.hotel.system.repository.RentalDetailRepository;
import com.hotel.system.repository.UsersRepository;
import com.hotel.system.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidentService {
    
    private final IncidentRepository incidentRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final UsersRepository usersRepository;
    private final BillingService billingService;

    @Transactional
public void createIncident(IncidentCreateRequest request, String username) {
    if (request.getName() == null || request.getName().isBlank()) {
        throw new RuntimeException("Tên sự cố không được để trống");
    }
    if (request.getOccurTime() == null) {
        throw new RuntimeException("Thời gian xảy ra sự cố không hợp lệ");
    }
    if (request.getRentalDetailId() == null || request.getRentalDetailId().isBlank()) {
        throw new RuntimeException("Vui lòng chọn phòng đang lưu trú xảy ra sự cố");
    }

    RentalDetail rentalDetail = rentalDetailRepository.findById(request.getRentalDetailId())
        .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết thuê phòng"));

    if (rentalDetail.getStatus() != com.hotel.system.entity.enums.RentalDetailStatus.CHECKED_IN
            && rentalDetail.getStatus() != com.hotel.system.entity.enums.RentalDetailStatus.OVERDUE) {
        throw new RuntimeException("Chỉ có thể báo cáo sự cố cho phòng đang lưu trú.");
    }

    if (rentalDetail.getRental() == null || billingService.hasSettlementBill(rentalDetail.getRental().getId())) {
        throw new RuntimeException("Đơn thuê này đã tất toán, không thể tạo thêm sự cố.");
    }

    Users creator = usersRepository.findByEmail(username)
             .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên"));

    Incident incident = new Incident();
    incident.setId(AppUtils.generateId("ICD", 10));
    incident.setName(request.getName().trim());
    incident.setOccurTime(request.getOccurTime());
    incident.setDescription(request.getDescription());
    incident.setStatus(IncidentStatus.PENDING);
    incident.setResponsible("HOTEL");
    incident.setMoney(0.0);
    incident.setRentalDetail(rentalDetail);
    incident.setCreatedBy(creator);

    incidentRepository.save(incident);
}

    @Transactional
    public void updateIncident(String incidentId, IncidentUpdateRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố"));

        if (request.getResponsible() != null && !request.getResponsible().isBlank()) {
            String resp = request.getResponsible().trim().toUpperCase();
            if (!resp.equals("CUSTOMER") && !resp.equals("HOTEL") && !resp.equals("NONE")) {
                throw new RuntimeException("Đối tượng chịu trách nhiệm không hợp lệ (chỉ nhận CUSTOMER, HOTEL hoặc NONE)");
            }
            incident.setResponsible(resp);
        }

        if ("NONE".equalsIgnoreCase(incident.getResponsible())) {
            incident.setMoney(0.0);
        } else if (request.getMoney() != null) {
            if (request.getMoney() < 0) {
                throw new RuntimeException("Số tiền sự cố không được âm");
            }
            incident.setMoney(request.getMoney());
        }

        if (request.getHandle() != null) incident.setHandle(request.getHandle());
        if (request.getStatus() != null) incident.setStatus(request.getStatus());

        incidentRepository.save(incident);
    }
}