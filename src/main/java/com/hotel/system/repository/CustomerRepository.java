package com.hotel.system.repository;

import com.hotel.system.entity.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    @Override
    @EntityGraph(attributePaths = {"user"})
    List<Customer> findAll();

    @EntityGraph(attributePaths = {"user"})
    List<Customer> findAllByOrderByUserCreateDateDesc();
}