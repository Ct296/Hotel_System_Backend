package com.hotel.system.repository;

import com.hotel.system.entity.RentalDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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


    @Query("""
        select rt.name, count(rd), coalesce(sum(b.totalAmount), 0)
        from RentalDetail rd
        join rd.room room
        join room.roomType rt
        left join Bill b on b.rental.id = rd.rental.id and b.type = 'FINAL'
        where extract(month from rd.checkinDate) = :month and extract(year from rd.checkinDate) = :year
        group by rt.name
        order by count(rd) desc
    """)
    List<Object[]> getTopRoomTypesByMonth(@Param("month") int month, @Param("year") int year);

    @Query(value = """
        select extract(isodow from RENTAL_DETAIL_CheckinDate) as dow, count(*) 
        from RENTAL_DETAIL 
        where RENTAL_DETAIL_CheckinDate >= current_date - interval '7 days'
        group by dow 
        order by dow
    """, nativeQuery = true)
    List<Object[]> getWeeklyBookingCounts();
}