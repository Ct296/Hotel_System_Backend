package com.hotel.system.repository;

import com.hotel.system.entity.AppliedPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppliedPeriodRepository extends JpaRepository<AppliedPeriod, String> {
    List<AppliedPeriod> findAllByOrderByStartDateDesc();
    long countByPriceRateId(String priceRateId);
    List<AppliedPeriod> findByPriceRateIdOrderByStartDateDesc(String priceRateId);
    List<AppliedPeriod> findByRoomTypeIdOrderByStartDateDesc(String roomTypeId);
    long countByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDateTime now1, LocalDateTime now2);


}