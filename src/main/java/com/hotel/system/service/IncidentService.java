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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final UsersRepository usersRepository;

    // Hàm tự sinh ID: ICD + 7 số (ví dụ ICD0000004)
    private String generateIncidentId() {
        long count = incidentRepository.count() + 1;
        return String.format("ICD%07d", count);
    }

    @Transactional
    public void createIncident(IncidentCreateRequest request, String username) {
        RentalDetail rentalDetail = rentalDetailRepository.findById(request.getRentalDetailId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết thuê phòng"));
        
        // Giả sử đăng nhập dùng Email để tìm User (sửa lại theo cơ chế Auth thực tế của bạn)
        Users creator = usersRepository.findByEmail(username) 
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên"));

        Incident incident = new Incident();
        incident.setId(generateIncidentId()); // Tự sinh ID
        incident.setName(request.getName());
        incident.setOccurTime(request.getOccurTime());
        incident.setDescription(request.getDescription());
        incident.setStatus(IncidentStatus.PROCESSING);
        incident.setRentalDetail(rentalDetail);
        incident.setCreatedBy(creator); // Gắn ID lễ tân

        incidentRepository.save(incident);
    }

    @Transactional
    public void updateIncident(String incidentId, IncidentUpdateRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố"));

        // Manager điền thông tin nào thì update thông tin đó
        if (request.getResponsible() != null) incident.setResponsible(request.getResponsible());
        if (request.getMoney() != null) incident.setMoney(request.getMoney());
        if (request.getHandle() != null) incident.setHandle(request.getHandle());
        if (request.getStatus() != null) incident.setStatus(request.getStatus());

        incidentRepository.save(incident);
    }
}