package com.hotel.system.repository;

import com.hotel.system.entity.Bill;
import com.hotel.system.entity.RentalDetail;
import com.hotel.system.entity.enums.BillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, String> {

    List<Bill> findAllByOrderByCreateDateDesc();

    List<Bill> findByRentalIdOrderByCreateDateDesc(String rentalId);

    boolean existsByRentalIdAndType(String rentalId, BillType type);

    // 1. Tìm thời gian Check-out gần nhất của phòng
    @Query("""
            select b.rental.customer.id, coalesce(sum(b.totalAmount), 0)
            from Bill b
            group by b.rental.customer.id
            """)
    List<Object[]> sumTotalAmountGroupByCustomerId();

    // 2. Tìm tổng doanh thu của khách hàng theo ID
    @Query("""
            select coalesce(sum(b.totalAmount), 0)
            from Bill b
            where b.rental.customer.id = :customerId
            """)
    Double sumTotalAmountByCustomerId(@Param("customerId") String customerId);

    // 3. Tìm tổng doanh thu của khách hàng theo ID và loại hóa đơn
    @Query(value = "SELECT EXTRACT(MONTH FROM BILL_CreateDate) as month, SUM(BILL_TotalAmount) as total " +
                   "FROM BILL " +
                   "WHERE EXTRACT(YEAR FROM BILL_CreateDate) = :year AND BILL_Type = 'FINAL' " +
                   "GROUP BY EXTRACT(MONTH FROM BILL_CreateDate) " +
                   "ORDER BY month", nativeQuery = true)
    List<Object[]> sumRevenueByMonth(@Param("year") int year);

    // 4. Tìm tổng doanh thu của khách hàng theo ID và loại hóa đơn
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE CAST(b.createDate AS date) = CURRENT_DATE")
    Double calculateTodayRevenue();

    // 5. Tìm tổng doanh thu của khách hàng theo ID và loại hóa đơn
    @Query(value = "SELECT EXTRACT(MONTH FROM BILL_CreateDate) as month, SUM(BILL_TotalAmount) FROM BILL WHERE EXTRACT(YEAR FROM BILL_CreateDate) = :year GROUP BY EXTRACT(MONTH FROM BILL_CreateDate)", nativeQuery = true)
    List<Object[]> getMonthlyRevenueByYear(@Param("year") int year);

    
    // 6. Tìm tổng doanh thu theo loại phòng
    @Query("""
        select rt.name, coalesce(sum(b.totalAmount), 0)
        from Bill b
        join b.rental r
        join r.rentalDetails rd
        join rd.room room
        join room.roomType rt
        where b.type = 'FINAL'
        group by rt.name
    """)
    
    // 7. Tìm tổng doanh thu theo loại phòng
    List<Object[]> sumRevenueByRoomType();
        
        @Query("""
        SELECT MAX(b.createDate)
        FROM Bill b
        JOIN b.rental r
        JOIN r.rentalDetails rd
        WHERE rd.room.id = :roomId
        AND b.type IN (com.hotel.system.entity.enums.BillType.FINAL, com.hotel.system.entity.enums.BillType.EARLY_CHECKOUT)
    """)
    LocalDateTime findLatestCheckoutTimeByRoomId(@Param("roomId") String roomId);
}
