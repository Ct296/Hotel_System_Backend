package com.hotel.system.repository;

import com.hotel.system.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    Optional<Review> findByCustomerId(String customerId);

    boolean existsByCustomerId(String customerId);

    List<Review> findAllByOrderByUpdateDateDesc();
}
