package com.hotel.system.repository;

import com.hotel.system.entity.Bill;
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

    // 1. Tổng chi tiêu thực tế theo từng khách hàng (Thu - Hoàn)
    @Query("""
            select b.rental.customer.id,
                   coalesce(sum(case when b.type = com.hotel.system.entity.enums.BillType.REFUND then -b.totalAmount else b.totalAmount end), 0)
            from Bill b
            group by b.rental.customer.id
            """)
    List<Object[]> sumTotalAmountGroupByCustomerId();

    // 2. Tổng chi tiêu thực tế của 1 khách hàng (Thu - Hoàn)
    @Query("""
            select coalesce(sum(case when b.type = com.hotel.system.entity.enums.BillType.REFUND then -b.totalAmount else b.totalAmount end), 0)
            from Bill b
            where b.rental.customer.id = :customerId
            """)
    Double sumTotalAmountByCustomerId(@Param("customerId") String customerId);

    // 3. Doanh thu ròng theo tháng (Thu - Hoàn)
    @Query(value = "SELECT EXTRACT(MONTH FROM BILL_CreateDate) as month, " +
                   "COALESCE(SUM(CASE WHEN BILL_Type = 'REFUND' THEN -BILL_TotalAmount ELSE BILL_TotalAmount END), 0) as total " +
                   "FROM BILL " +
                   "WHERE EXTRACT(YEAR FROM BILL_CreateDate) = :year " +
                   "GROUP BY EXTRACT(MONTH FROM BILL_CreateDate) " +
                   "ORDER BY month", nativeQuery = true)
    List<Object[]> sumRevenueByMonth(@Param("year") int year);

    // 4. Doanh thu ròng hôm nay (Thu - Hoàn)
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN b.type = com.hotel.system.entity.enums.BillType.REFUND THEN -b.totalAmount ELSE b.totalAmount END), 0)
            FROM Bill b
            WHERE CAST(b.createDate AS date) = CURRENT_DATE
            """)
    Double calculateTodayRevenue();

    // 5. Doanh thu ròng theo tháng trong năm (Thu - Hoàn)
    @Query(value = "SELECT EXTRACT(MONTH FROM BILL_CreateDate) as month, " +
                   "COALESCE(SUM(CASE WHEN BILL_Type = 'REFUND' THEN -BILL_TotalAmount ELSE BILL_TotalAmount END), 0) " +
                   "FROM BILL " +
                   "WHERE EXTRACT(YEAR FROM BILL_CreateDate) = :year " +
                   "GROUP BY EXTRACT(MONTH FROM BILL_CreateDate)", nativeQuery = true)
    List<Object[]> getMonthlyRevenueByYear(@Param("year") int year);

    // 6. Doanh thu theo loại phòng (Thu - Hoàn)
    @Query("""
            select rt.name,
                   coalesce(sum(case when b.type = com.hotel.system.entity.enums.BillType.REFUND then -b.totalAmount else b.totalAmount end), 0)
            from Bill b
            join b.rental r
            join r.rentalDetails rd
            join rd.room room
            join room.roomType rt
            where b.type in (com.hotel.system.entity.enums.BillType.FINAL, com.hotel.system.entity.enums.BillType.REFUND)
              and rd.status <> com.hotel.system.entity.enums.RentalDetailStatus.CANCELLED
            group by rt.name
            """)
    List<Object[]> sumRevenueByRoomType();

    // 7. Thời gian Check-out / đóng đơn gần nhất của phòng
    @Query("""
            SELECT MAX(b.createDate)
            FROM Bill b
            JOIN b.rental r
            JOIN r.rentalDetails rd
            WHERE rd.room.id = :roomId
              AND b.type IN (com.hotel.system.entity.enums.BillType.FINAL, com.hotel.system.entity.enums.BillType.REFUND)
            """)
    LocalDateTime findLatestCheckoutTimeByRoomId(@Param("roomId") String roomId);
}