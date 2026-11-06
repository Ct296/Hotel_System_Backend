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
import com.hotel.system.util.AppUtils; // <-- Đã import thư viện sinh ID của bạn
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidentService {
    
    private final IncidentRepository incidentRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public void createIncident(IncidentCreateRequest request, String username) {
        RentalDetail rentalDetail = null;
        if (request.getRentalDetailId() != null && !request.getRentalDetailId().isBlank()) {
            rentalDetail = rentalDetailRepository.findById(request.getRentalDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết thuê phòng"));
        }
                
        Users creator = usersRepository.findByEmail(username)
                 .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên"));

        // 3. Ghi nhận sự cố mới
        Incident incident = new Incident();
        incident.setId(AppUtils.generateId("ICD", 10)); 
        incident.setName(request.getName());
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

        // Manager chọn thông tin nào thì update thông tin đó
        if (request.getResponsible() != null) incident.setResponsible(request.getResponsible());
        if (request.getMoney() != null) incident.setMoney(request.getMoney());
        if (request.getHandle() != null) incident.setHandle(request.getHandle());
        if (request.getStatus() != null) incident.setStatus(request.getStatus());

        incidentRepository.save(incident);
    }
}