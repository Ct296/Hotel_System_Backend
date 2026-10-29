package com.hotel.system.controller;

import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.AccountState;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.repository.*;
import com.hotel.system.service.AdminAccountService;
import com.hotel.system.service.CustomerDashboardService;
import com.hotel.system.service.CustomerTierService;
import com.hotel.system.service.ai.ReviewAiExportService;
import com.hotel.system.service.ai.ReviewAiDashboardService;
import com.hotel.system.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.hotel.system.service.ai.ReviewAiInferenceClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/manager/customer")
@RequiredArgsConstructor
public class ManagerCustomerController {

    private final CustomerRepository customerRepository;
    private final TierCustomerRepository tierCustomerRepository;
    private final TierHistoryRepository tierHistoryRepository;
    private final ReviewRepository reviewRepository;
    private final UsersRepository usersRepository;
    private final CustomerTierService customerTierService;
    private final ManagerRepository managerRepository;
    private final ReviewAiExportService reviewAiExportService;
    private final ReviewAiDashboardService reviewAiDashboardService;
    private final CustomerDashboardService customerDashboardService;
    private final ReviewAiInferenceClient reviewAiInferenceClient;
    private final AdminAccountService adminAccountService;
    private final AccountStatusRepository accountStatusRepository;

    private boolean hasCustomerAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) return true;
        if (auth.getPrincipal() instanceof UserDetails userDetails) {
            Users user = usersRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (user != null && user.getRole() == Role.MANAGER) {
                Manager manager = managerRepository.findById(user.getId()).orElse(null);
                return manager != null && manager.getTitle().name().equals("CUSTOMER_MANAGER");
            }
        }
        return false;
    }

    // Hàm Helper lấy trạng thái mới nhất
    private AccountStatus getEffectiveLatestStatus(String userId) {
        List<AccountStatus> statuses = accountStatusRepository.findAll()
                .stream()
                .filter(status -> status.getAccount() != null
                        && status.getAccount().getId() != null
                        && status.getAccount().getId().equals(userId))
                .sorted(Comparator.comparing(AccountStatus::getStartTime))
                .toList();
        if (statuses.isEmpty()) return null;
        AccountStatus latestStatus = statuses.get(statuses.size() - 1);
        if (latestStatus.getName() == AccountState.LOCKED
                && latestStatus.getEndTime() != null
                && latestStatus.getEndTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        return latestStatus;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        if (!hasCustomerAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));

        List<Customer> customers = customerRepository.findAllByOrderByUserCreateDateDesc();
        List<TierCustomer> tierCustomers = tierCustomerRepository.findAllByOrderByConditionAsc();
        List<TierHistory> tierHistories = tierHistoryRepository.findAllByOrderByStartDateDesc();
        List<Review> reviews = reviewRepository.findAllByOrderByUpdateDateDesc();

        Map<String, TierCustomer> currentTierByCustomerId = customerDashboardService.buildCurrentTierByCustomerId(customers);
        Map<String, Double> totalSpendingByCustomerId = customerDashboardService.buildTotalSpendingByCustomerId(customers);

        Map<String, String> accountStatusByCustomerId = new HashMap<>();
        for (Customer c : customers) {
            AccountStatus effectiveStatus = getEffectiveLatestStatus(c.getId());
            AccountState state = (effectiveStatus != null && effectiveStatus.getName() != null) ? effectiveStatus.getName() : AccountState.ACTIVE;
            accountStatusByCustomerId.put(c.getId(), state.name());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("customers", customers);
        response.put("tierCustomers", tierCustomers);
        response.put("tierHistories", tierHistories);
        response.put("reviews", reviews);
        response.put("customerCount", customers.size());
        response.put("tierCustomerCount", tierCustomers.size());
        response.put("activeMemberCount", tierHistories.stream().filter(item -> item.getEndDate() == null).count());
        
        response.put("averageReviewRate", customerDashboardService.calculateAverageReviewRate(reviews));
        response.put("customerTierSummary", customerDashboardService.buildCustomerTierSummary(tierCustomers, tierHistories));
        response.put("reviewRateSummary", customerDashboardService.buildReviewRateSummary(reviews));
        
        response.put("currentTierByCustomerId", currentTierByCustomerId);
        response.put("totalSpendingByCustomerId", totalSpendingByCustomerId);
        response.put("accountStatusByCustomerId", accountStatusByCustomerId);

        ReviewAiDashboardService.ReviewAiDashboardSummary aiSummary = reviewAiDashboardService.buildSummary();
        response.put("aiSummary", aiSummary);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/tier-customers")
    @Transactional
    public ResponseEntity<?> saveTierCustomer(@RequestBody Map<String, Object> payload) {
        if (!hasCustomerAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        String tierCustomerId = (String) payload.get("tierCustomerId");
        String name = (String) payload.get("name");
        Double condition = Double.valueOf(payload.get("condition").toString());
        BigDecimal discount = new BigDecimal(payload.get("discount").toString());
        String benefit = (String) payload.get("benefit");

        String trimmedName = name == null ? "" : name.trim();
        String trimmedBenefit = benefit == null ? "" : benefit.trim();

        if (trimmedName.isBlank() || condition < 0 || discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dữ liệu nhập vào không hợp lệ."));
        }

        boolean duplicated = tierCustomerRepository.findAllByOrderByConditionAsc().stream()
                .anyMatch(item -> item.getName() != null && item.getName().equalsIgnoreCase(trimmedName) 
                        && (tierCustomerId == null || tierCustomerId.isBlank() || !item.getId().equals(tierCustomerId)));

        if (duplicated) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tên hạng khách hàng đã tồn tại."));
        }

        boolean isCreate = tierCustomerId == null || tierCustomerId.isBlank();
        TierCustomer tierCustomer = isCreate ? new TierCustomer() : tierCustomerRepository.findById(tierCustomerId).orElse(null);
        if (tierCustomer == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy hạng."));

        if (isCreate) tierCustomer.setId(AppUtils.generateId("TIC", 10));
        tierCustomer.setName(trimmedName);
        tierCustomer.setCondition(condition);
        tierCustomer.setBenefit(trimmedBenefit.isBlank() ? null : trimmedBenefit);
        tierCustomer.setDiscount(discount.setScale(2, RoundingMode.HALF_UP));
        tierCustomerRepository.save(tierCustomer);

        return ResponseEntity.ok(Map.of("message", isCreate ? "Đã thêm hạng khách hàng mới." : "Đã cập nhật hạng khách hàng."));
    }

    @PostMapping("/customers/{id}/change-tier")
    public ResponseEntity<?> changeCustomerTier() {
        return ResponseEntity.badRequest().body(Map.of("error", "Không được đổi hạng khách hàng thủ công. Hạng sẽ tự động tính theo tổng bill."));
    }

    @PostMapping("/ai/export")
    public ResponseEntity<?> exportReviewAiAnalysis() {
        if (!hasCustomerAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            ReviewAiExportService.ReviewAiExportSummary summary = reviewAiExportService.exportLatestReviews();
            return ResponseEntity.ok(Map.of(
                "message", "Đã quét và phân tích review. Thêm mới: " + summary.inserted() + ", Cập nhật: " + summary.updated() + ", Bỏ qua: " + summary.skipped(),
                "summary", summary
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi chạy AI export: " + e.getMessage()));
        }
    }

    @PostMapping("/ai/analyze-single")
    public ResponseEntity<?> analyzeSingleReview(@RequestBody Map<String, String> payload) {
        if (!hasCustomerAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            String text = payload.getOrDefault("text", "");
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.util.Map response = restTemplate.postForObject(
                    "http://localhost:8000/api/analyze", 
                    java.util.Map.of("text", text), 
                    java.util.Map.class
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Lỗi kết nối Server AI: " + e.getMessage()));
        }
    }

    // 
    @PutMapping("/{id}/toggle-lock")
    public ResponseEntity<?> toggleCustomerLock(@PathVariable String id) {
        if (!hasCustomerAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        try {
            // Lấy thông tin User để kiểm tra quyền
            Users targetUser = usersRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

            // Bắt buộc Role phải là CUSTOMER
            if (targetUser.getRole() != Role.CUSTOMER) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Không có quyền thao tác! Bạn chỉ được phép khoá/mở khoá tài khoản Khách hàng."));
            }

            String userId = targetUser.getId();
            
            // Xử lý logic khoá/mở khoá
            AccountStatus effectiveStatus = getEffectiveLatestStatus(userId);
            boolean isCurrentlyLocked = effectiveStatus != null && effectiveStatus.getName() == AccountState.LOCKED;

            if (isCurrentlyLocked) {
                adminAccountService.unlockAccount(userId, "Manager Customer mở khoá");
                return ResponseEntity.ok(Map.of("message", "Đã mở khoá tài khoản khách hàng."));
            } else {
                adminAccountService.lockAccount(userId, -1, "Manager Customer khoá");
                return ResponseEntity.ok(Map.of("message", "Đã khoá tài khoản khách hàng."));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}