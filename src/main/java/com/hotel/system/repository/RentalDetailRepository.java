package com.hotel.system.repository;

import com.hotel.system.entity.RentalDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RentalDetailRepository extends JpaRepository<RentalDetail, String> {
    // 1. Số phòng Check-in hôm nay
    @Query("SELECT COUNT(rd) FROM RentalDetail rd WHERE CAST(rd.checkinDate AS date) = CURRENT_DATE")
    Long countTodayArrivals();

    // 2. Số phòng Check-out hôm nay (CheckinDate + LengthOfStay)
    @Query(value = "SELECT COUNT(*) FROM RENTAL_DETAIL WHERE DATE(RENTAL_DETAIL_CheckinDate + RENTAL_DETAIL_LengthOfStay * INTERVAL '1 day') = CURRENT_DATE", nativeQuery = true)
    Long countTodayDepartures();

    // 3. Danh sách phòng Check-in / Check-out hôm nay
    @Query(value = "SELECT * FROM RENTAL_DETAIL WHERE DATE(RENTAL_DETAIL_CheckinDate) = CURRENT_DATE OR DATE(RENTAL_DETAIL_CheckinDate + RENTAL_DETAIL_LengthOfStay * INTERVAL '1 day') = CURRENT_DATE", nativeQuery = true)
    List<RentalDetail> findTodayTasks();
}