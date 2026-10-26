package com.hotel.system.repository;

import com.hotel.system.entity.Bill;
import com.hotel.system.entity.RentalDetail;
import com.hotel.system.entity.enums.BillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, String> {

    List<Bill> findAllByOrderByCreateDateDesc();

    List<Bill> findByRentalIdOrderByCreateDateDesc(String rentalId);

    boolean existsByRentalIdAndType(String rentalId, BillType type);

    @Query("""
            select b.rental.customer.id, coalesce(sum(b.totalAmount), 0)
            from Bill b
            group by b.rental.customer.id
            """)
    List<Object[]> sumTotalAmountGroupByCustomerId();

    @Query("""
            select coalesce(sum(b.totalAmount), 0)
            from Bill b
            where b.rental.customer.id = :customerId
            """)
    Double sumTotalAmountByCustomerId(@Param("customerId") String customerId);

    @Query(value = "SELECT EXTRACT(MONTH FROM BILL_CreateDate) as month, SUM(BILL_TotalAmount) as total " +
                   "FROM BILL " +
                   "WHERE EXTRACT(YEAR FROM BILL_CreateDate) = :year AND BILL_Type = 'FINAL' " +
                   "GROUP BY EXTRACT(MONTH FROM BILL_CreateDate) " +
                   "ORDER BY month", nativeQuery = true)
    List<Object[]> sumRevenueByMonth(@Param("year") int year);

// 1. Tổng doanh thu hôm nay
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE CAST(b.createDate AS date) = CURRENT_DATE")
    Double calculateTodayRevenue();

    // 2. Doanh thu 12 tháng
    @Query(value = "SELECT EXTRACT(MONTH FROM BILL_CreateDate) as month, SUM(BILL_TotalAmount) FROM BILL WHERE EXTRACT(YEAR FROM BILL_CreateDate) = :year GROUP BY EXTRACT(MONTH FROM BILL_CreateDate)", nativeQuery = true)
    List<Object[]> getMonthlyRevenueByYear(@Param("year") int year);

    // 3. Danh sách phòng Check-in / Check-out hôm nay
    @Query(value = "SELECT * FROM RENTAL_DETAIL WHERE DATE(RENTAL_DETAIL_CheckinDate) = CURRENT_DATE OR DATE(RENTAL_DETAIL_CheckinDate + RENTAL_DETAIL_LengthOfStay * INTERVAL '1 day') = CURRENT_DATE", nativeQuery = true)
    List<RentalDetail> findTodayTasks();

    
}
