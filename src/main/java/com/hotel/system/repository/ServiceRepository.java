package com.hotel.system.repository;

import com.hotel.system.entity.Service;
import com.hotel.system.entity.enums.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, String> {

    List<Service> findAllByOrderByCreateDateDesc();

    List<Service> findByStatusOrderByCreateDateDesc(ServiceStatus status);

    boolean existsByNameIgnoreCase(String name);
}