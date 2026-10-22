package com.hotel.system.service;

import com.hotel.system.entity.*;
import com.hotel.system.repository.BillRepository;
import com.hotel.system.repository.TierHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerDashboardService {
    
    private final BillRepository billRepository;
    private final TierHistoryRepository tierHistoryRepository;

    public BigDecimal calculateAverageReviewRate(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        double average = reviews.stream()
                .filter(item -> item.getRate() != null)
                .mapToInt(Review::getRate)
                .average()
                .orElse(0.0);
        return BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, Long> buildCustomerTierSummary(List<TierCustomer> tierCustomers, List<TierHistory> tierHistories) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (TierCustomer tierCustomer : tierCustomers) {
            long count = tierHistories.stream()
                    .filter(item -> item.getEndDate() == null)
                    .filter(item -> item.getTierCustomer() != null && tierCustomer.getId().equals(item.getTierCustomer().getId()))
                    .count();
            result.put(tierCustomer.getName(), count);
        }
        return result;
    }

    public Map<Integer, Long> buildReviewRateSummary(List<Review> reviews) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        for (int rate = 5; rate >= 1; rate--) {
            int currentRate = rate;
            long count = reviews.stream()
                    .filter(item -> item.getRate() != null && item.getRate() == currentRate)
                    .count();
            result.put(rate, count);
        }
        return result;
    }

    public Map<String, TierCustomer> buildCurrentTierByCustomerId(List<Customer> customers) {
        Map<String, TierCustomer> result = new LinkedHashMap<>();
        for (Customer customer : customers) {
            TierHistory currentHistory = findTierHistoryEffectiveAt(customer.getId(), LocalDateTime.now());
            if (currentHistory != null) {
                result.put(customer.getId(), currentHistory.getTierCustomer());
            }
        }
        return result;
    }

    public Map<String, Double> buildTotalSpendingByCustomerId(List<Customer> customers) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Customer customer : customers) {
            result.put(customer.getId(), 0.0);
        }
        for (Object[] row : billRepository.sumTotalAmountGroupByCustomerId()) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            String customerId = String.valueOf(row[0]);
            double totalSpending = row[1] instanceof Number ? ((Number) row[1]).doubleValue() : 0.0;
            result.put(customerId, totalSpending);
        }
        return result;
    }

    private TierHistory findTierHistoryEffectiveAt(String customerId, LocalDateTime targetTime) {
        if (customerId == null || customerId.isBlank() || targetTime == null) {
            return null;
        }
        TierHistory currentOpenHistory = tierHistoryRepository
                .findTopByCustomerIdAndStartDateLessThanEqualAndEndDateIsNullOrderByStartDateDesc(customerId, targetTime)
                .orElse(null);
        TierHistory currentClosedHistory = tierHistoryRepository
                .findTopByCustomerIdAndStartDateLessThanEqualAndEndDateGreaterThanOrderByStartDateDesc(customerId, targetTime, targetTime)
                .orElse(null);

        if (currentOpenHistory == null) return currentClosedHistory;
        if (currentClosedHistory == null) return currentOpenHistory;

        return currentClosedHistory.getStartDate().isAfter(currentOpenHistory.getStartDate()) ? currentClosedHistory : currentOpenHistory;
    }
}