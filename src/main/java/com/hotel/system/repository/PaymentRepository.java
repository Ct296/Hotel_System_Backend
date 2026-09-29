package com.hotel.system.repository;

import com.hotel.system.entity.Payment;
import com.hotel.system.entity.enums.BillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findAllByOrderByDateDesc();

    List<Payment> findByBillRentalIdOrderByDateDesc(String rentalId);

    Optional<Payment> findByBillId(String billId);

    boolean existsByBillRentalIdAndBillType(String rentalId, BillType billType);
}
