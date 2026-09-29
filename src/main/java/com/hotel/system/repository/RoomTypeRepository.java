package com.hotel.system.repository;

import com.hotel.system.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, String> {
    RoomType findByName(String name);
    List<RoomType> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}