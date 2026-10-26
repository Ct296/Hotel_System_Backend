package com.hotel.system.repository;

import com.hotel.system.entity.ServiceUsage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.List;

@Repository
public interface ServiceUsageRepository extends JpaRepository<ServiceUsage, String> {

    List<ServiceUsage> findAllByOrderByTimeDesc();

    List<ServiceUsage> findByServiceIdOrderByTimeDesc(String serviceId);

    long countByServiceId(String serviceId);

    // 1. Top dịch vụ bán chạy
    @Query("SELECT s.name, SUM(su.count), SUM(su.count * su.unitPrice) " +
           "FROM ServiceUsage su JOIN su.service s " +
           "WHERE EXTRACT(MONTH FROM su.time) = EXTRACT(MONTH FROM CURRENT_DATE) " +
           "AND EXTRACT(YEAR FROM su.time) = EXTRACT(YEAR FROM CURRENT_DATE) " +
           "GROUP BY s.id, s.name ORDER BY SUM(su.count) DESC")
    List<Object[]> getTop5ServicesThisMonth(Pageable pageable);

    // 2. Lượt dùng dịch vụ cụ thể theo 12 tháng
    @Query(value = "SELECT EXTRACT(MONTH FROM SERVICE_USAGE_Time) as month, SUM(SERVICE_USAGE_Count) FROM SERVICE_USAGE WHERE SERVICE_ID = :serviceId AND EXTRACT(YEAR FROM SERVICE_USAGE_Time) = :year GROUP BY EXTRACT(MONTH FROM SERVICE_USAGE_Time)", nativeQuery = true)
    List<Object[]> getMonthlyServiceUsage(@Param("serviceId") String serviceId, @Param("year") int year);
    
    // 3. Tổng lượt dùng tất cả dịch vụ 12 tháng
    @Query(value = "SELECT EXTRACT(MONTH FROM SERVICE_USAGE_Time) as month, SUM(SERVICE_USAGE_Count) FROM SERVICE_USAGE WHERE EXTRACT(YEAR FROM SERVICE_USAGE_Time) = :year GROUP BY EXTRACT(MONTH FROM SERVICE_USAGE_Time)", nativeQuery = true)
    List<Object[]> getMonthlyAllServicesUsage(@Param("year") int year);
}