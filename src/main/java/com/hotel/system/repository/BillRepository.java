package com.hotel.system.repository;

import com.hotel.system.entity.Bill;
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
}
