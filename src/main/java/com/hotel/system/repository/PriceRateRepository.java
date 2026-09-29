package com.hotel.system.repository;

import com.hotel.system.entity.PriceRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceRateRepository extends JpaRepository<PriceRate, String> {
    List<PriceRate> findAllByOrderByCreateDateDesc();
    boolean existsByEventNameIgnoreCase(String eventName);
}