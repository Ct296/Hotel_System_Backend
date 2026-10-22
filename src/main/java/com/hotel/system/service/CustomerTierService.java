package com.hotel.system.service;

import com.hotel.system.entity.Customer;
import com.hotel.system.entity.TierCustomer;
import com.hotel.system.entity.TierHistory;
import com.hotel.system.repository.BillRepository;
import com.hotel.system.repository.TierCustomerRepository;
import com.hotel.system.repository.TierHistoryRepository;
import com.hotel.system.util.AppUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerTierService {

    private final TierCustomerRepository tierCustomerRepository;
    private final TierHistoryRepository tierHistoryRepository;
    private final BillRepository billRepository;

    public CustomerTierService(TierCustomerRepository tierCustomerRepository,
                               TierHistoryRepository tierHistoryRepository,
                               BillRepository billRepository) {
        this.tierCustomerRepository = tierCustomerRepository;
        this.tierHistoryRepository = tierHistoryRepository;
        this.billRepository = billRepository;
    }

    public void syncCustomerTierByBills(Customer customer) {
        if (customer == null || customer.getId() == null) return;
        List<TierCustomer> tiers = tierCustomerRepository.findAllByOrderByConditionAsc();
        if (tiers.isEmpty()) return;

        double totalSpending = resolveCustomerTotalSpending(customer.getId());
        TierCustomer eligibleTier = resolveEligibleTier(tiers, totalSpending);
        if (eligibleTier == null) eligibleTier = tiers.get(0);

        LocalDateTime now = LocalDateTime.now();
        TierHistory currentHistory = findTierHistoryEffectiveAt(customer.getId(), now);

        if (currentHistory == null) {
            createNewTierHistory(customer, eligibleTier, totalSpending, now);
            return;
        }

        currentHistory.setTotalSpending(totalSpending);
        String currentTierId = currentHistory.getTierCustomer() == null ? null : currentHistory.getTierCustomer().getId();
        if (currentTierId != null && currentTierId.equals(eligibleTier.getId())) {
            currentHistory.setReason(buildTierReason(eligibleTier, totalSpending, false));
            tierHistoryRepository.save(currentHistory);
            return;
        }

        currentHistory.setEndDate(now);
        currentHistory.setReason(buildTierReason(currentHistory.getTierCustomer(), totalSpending, true));
        tierHistoryRepository.save(currentHistory);

        createNewTierHistory(customer, eligibleTier, totalSpending, now);
    }

    private void createNewTierHistory(Customer customer, TierCustomer tier, double totalSpending, LocalDateTime now) {
        TierHistory newHistory = new TierHistory();
        newHistory.setId(AppUtils.generateId("THI", 10));
        newHistory.setStartDate(now);
        newHistory.setEndDate(null);
        newHistory.setTotalSpending(totalSpending);
        newHistory.setReason(buildTierReason(tier, totalSpending, false));
        newHistory.setCustomer(customer);
        newHistory.setTierCustomer(tier);
        tierHistoryRepository.save(newHistory);
    }

    private double resolveCustomerTotalSpending(String customerId) {
        if (customerId == null || customerId.isBlank()) return 0.0;
        Double total = billRepository.sumTotalAmountByCustomerId(customerId);
        return (total == null || total < 0) ? 0.0 : total;
    }

    private TierCustomer resolveEligibleTier(List<TierCustomer> tiers, double totalSpending) {
        TierCustomer eligibleTier = null;
        for (TierCustomer tier : tiers) {
            double condition = tier.getCondition() == null ? 0.0 : tier.getCondition();
            if (totalSpending >= condition) eligibleTier = tier;
        }
        return eligibleTier;
    }

    public TierHistory findTierHistoryEffectiveAt(String customerId, LocalDateTime targetTime) {
        if (customerId == null || customerId.isBlank() || targetTime == null) return null;
        TierHistory currentOpenHistory = tierHistoryRepository.findTopByCustomerIdAndStartDateLessThanEqualAndEndDateIsNullOrderByStartDateDesc(customerId, targetTime).orElse(null);
        TierHistory currentClosedHistory = tierHistoryRepository.findTopByCustomerIdAndStartDateLessThanEqualAndEndDateGreaterThanOrderByStartDateDesc(customerId, targetTime, targetTime).orElse(null);

        if (currentOpenHistory == null) return currentClosedHistory;
        if (currentClosedHistory == null) return currentOpenHistory;

        LocalDateTime openStart = currentOpenHistory.getStartDate();
        LocalDateTime closedStart = currentClosedHistory.getStartDate();
        if (openStart == null) return currentClosedHistory;
        if (closedStart == null) return currentOpenHistory;
        return closedStart.isAfter(openStart) ? currentClosedHistory : currentOpenHistory;
    }

    private String buildTierReason(TierCustomer tierCustomer, double totalSpending, boolean closedHistory) {
        String tierName = tierCustomer == null || tierCustomer.getName() == null || tierCustomer.getName().isBlank() ? "không xác định" : tierCustomer.getName().trim();
        String prefix = closedHistory ? "Đóng hạng do hệ thống tự rà bill: " : "Hệ thống tự rà bill, tổng chi: ";
        String message = prefix + AppUtils.formatMoney(totalSpending) + (closedHistory ? "" : " -> " + tierName);
        return message.length() > 100 ? message.substring(0, 100) : message;
    }
}