package com.hotel.system.repository;

import com.hotel.system.entity.TierCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TierCustomerRepository extends JpaRepository<TierCustomer, String> {

    List<TierCustomer> findAllByOrderByConditionAsc();

    boolean existsByNameIgnoreCase(String name);
}