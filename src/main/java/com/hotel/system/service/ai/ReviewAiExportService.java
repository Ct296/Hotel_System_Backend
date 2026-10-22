package com.hotel.system.service.ai;

import com.hotel.system.dto.ai.ReviewAiAnalysisRecord;
import com.hotel.system.dto.ai.ReviewAiInferenceResult;
import com.hotel.system.entity.Customer;
import com.hotel.system.entity.Review;
import com.hotel.system.entity.Users;
import com.hotel.system.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReviewAiExportService {

    private final ReviewRepository reviewRepository;
    private final ReviewAiInferenceClient reviewAiInferenceClient;
    private final ReviewAiJsonlStorageService reviewAiJsonlStorageService;

    public ReviewAiExportService(ReviewRepository reviewRepository,
                                 ReviewAiInferenceClient reviewAiInferenceClient,
                                 ReviewAiJsonlStorageService reviewAiJsonlStorageService) {
        this.reviewRepository = reviewRepository;
        this.reviewAiInferenceClient = reviewAiInferenceClient;
        this.reviewAiJsonlStorageService = reviewAiJsonlStorageService;
    }

    @Transactional(readOnly = true)
    public ReviewAiExportSummary exportLatestReviews() {
        List<Review> reviews = reviewRepository.findAllByOrderByUpdateDateDesc();
        Map<String, ReviewAiAnalysisRecord> existingMap = new LinkedHashMap<>(reviewAiJsonlStorageService.loadAllByReviewId());

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        for (Review review : reviews) {
            if (review == null) {
                continue;
            }
            String reviewId = safe(review.getId());
            String description = safe(review.getDescription());
            if (reviewId.isBlank() || description.isBlank()) {
                skipped++;
                continue;
            }

            ReviewAiAnalysisRecord existing = existingMap.get(reviewId);
            if (!needsAnalysis(review, existing)) {
                skipped++;
                continue;
            }

            ReviewAiInferenceResult result = reviewAiInferenceClient.analyze(description);
            ReviewAiAnalysisRecord newRecord = toRecord(review, result);
            if (existing == null) {
                inserted++;
            } else {
                updated++;
            }
            existingMap.put(reviewId, newRecord);
        }

        reviewAiJsonlStorageService.saveAll(existingMap);

        return new ReviewAiExportSummary(
                reviews.size(),
                inserted,
                updated,
                skipped,
                existingMap.size(),
                reviewAiJsonlStorageService.getOutputFile().toString(),
                LocalDateTime.now()
        );
    }

    private boolean needsAnalysis(Review review, ReviewAiAnalysisRecord existing) {
        if (existing == null) {
            return true;
        }
        if (!Objects.equals(safe(review.getDescription()), safe(existing.reviewText()))) {
            return true;
        }
        return !Objects.equals(review.getUpdateDate(), existing.reviewUpdateDate());
    }

    private ReviewAiAnalysisRecord toRecord(Review review, ReviewAiInferenceResult result) {
        Customer customer = review.getCustomer();
        Users user = customer != null ? customer.getUser() : null;
        String customerName = user == null
                ? null
                : (safe(user.getLastName()) + " " + safe(user.getFirstName())).trim();

        return new ReviewAiAnalysisRecord(
                review.getId(),
                customer != null ? customer.getId() : null,
                customerName,
                review.getRate(),
                safe(review.getDescription()),
                review.getUpdateDate(),
                LocalDateTime.now(),
                result.rawText(),
                result.normalizedText(),
                result.directSentimentLabel(),
                result.directSentimentScore(),
                result.directSentimentProbabilities(),
                safeList(result.mlAspectLabels()),
                safeList(result.lexiconAspectLabels()),
                safeList(result.aspectLabels()),
                result.mlAspectScore(),
                result.lexiconAspectScore(),
                result.aspectScore(),
                result.lexiconClauseScore(),
                result.finalScore(),
                result.finalLabel(),
                derivePrimaryAspect(result.aspectLabels())
        );
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String derivePrimaryAspect(List<String> aspectLabels) {
        if (aspectLabels == null || aspectLabels.isEmpty()) {
            return "khac";
        }
        return aspectLabels.stream()
                .filter(Objects::nonNull)
                .map(label -> label.contains("__") ? label.substring(0, label.indexOf("__")) : label)
                .filter(item -> !item.isBlank())
                .sorted(Comparator.naturalOrder())
                .findFirst()
                .orElse("khac");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record ReviewAiExportSummary(
            int totalReviewsInDb,
            int inserted,
            int updated,
            int skipped,
            int totalRecordsInFile,
            String outputFile,
            LocalDateTime executedAt
    ) {
    }
}
