package com.hotel.system.repository;

import com.hotel.system.entity.ServiceUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceUsageRepository extends JpaRepository<ServiceUsage, String> {

    List<ServiceUsage> findAllByOrderByTimeDesc();

    List<ServiceUsage> findByServiceIdOrderByTimeDesc(String serviceId);

    long countByServiceId(String serviceId);
}