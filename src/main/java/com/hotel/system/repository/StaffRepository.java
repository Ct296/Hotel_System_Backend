package com.hotel.system.repository;

import com.hotel.system.entity.Staff;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {

    @Override
    @EntityGraph(attributePaths = {"user"})
    List<Staff> findAll();

    @EntityGraph(attributePaths = {"user"})
    List<Staff> findAllByOrderByEmploymentTimeDesc();
}