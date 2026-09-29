package com.hotel.system.repository;

import com.hotel.system.entity.TierHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TierHistoryRepository extends JpaRepository<TierHistory, String> {

    List<TierHistory> findAllByOrderByStartDateDesc();

    List<TierHistory> findByCustomerIdOrderByStartDateDesc(String customerId);

    Optional<TierHistory> findTopByCustomerIdAndEndDateIsNullOrderByStartDateDesc(String customerId);

    Optional<TierHistory> findTopByCustomerIdAndStartDateLessThanEqualAndEndDateIsNullOrderByStartDateDesc(String customerId, LocalDateTime now);

    Optional<TierHistory> findTopByCustomerIdAndStartDateLessThanEqualAndEndDateGreaterThanOrderByStartDateDesc(String customerId, LocalDateTime now, LocalDateTime currentTimeForEndDate);
}