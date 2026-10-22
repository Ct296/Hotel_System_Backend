package com.hotel.system.service.ai;

import com.hotel.system.dto.ai.ReviewAiAnalysisRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReviewAiDashboardService {

    private final ReviewAiJsonlStorageService reviewAiJsonlStorageService;

    public ReviewAiDashboardService(ReviewAiJsonlStorageService reviewAiJsonlStorageService) {
        this.reviewAiJsonlStorageService = reviewAiJsonlStorageService;
    }

    public ReviewAiDashboardSummary buildSummary() {
        Map<String, ReviewAiAnalysisRecord> allMap = reviewAiJsonlStorageService.loadAllByReviewId();
        List<ReviewAiAnalysisRecord> records = allMap.values().stream().toList();

        long positiveCount = records.stream()
                .filter(item -> "positive".equalsIgnoreCase(safe(item.finalLabel())))
                .count();

        long negativeCount = records.stream()
                .filter(item -> "negative".equalsIgnoreCase(safe(item.finalLabel())))
                .count();

        long neutralCount = records.stream()
                .filter(item -> "neutral".equalsIgnoreCase(safe(item.finalLabel())))
                .count();

        Map<String, Long> positiveAspectSummary = new LinkedHashMap<>();
        Map<String, Long> negativeAspectSummary = new LinkedHashMap<>();

        for (ReviewAiAnalysisRecord record : records) {
            List<String> aspectLabels = record.aspectLabels();
            if (aspectLabels == null || aspectLabels.isEmpty()) {
                continue;
            }

            for (String label : aspectLabels) {
                if (label == null || label.isBlank()) {
                    continue;
                }

                String aspect = label;
                String sentiment = "";
                if (label.contains("__")) {
                    aspect = label.substring(0, label.indexOf("__"));
                    sentiment = label.substring(label.indexOf("__") + 2);
                }

                if ("positive".equalsIgnoreCase(sentiment)) {
                    positiveAspectSummary.put(aspect, positiveAspectSummary.getOrDefault(aspect, 0L) + 1L);
                } else if ("negative".equalsIgnoreCase(sentiment)) {
                    negativeAspectSummary.put(aspect, negativeAspectSummary.getOrDefault(aspect, 0L) + 1L);
                }
            }
        }

        Map<String, Long> sortedPositiveAspectSummary = sortDesc(positiveAspectSummary, 5);
        Map<String, Long> sortedNegativeAspectSummary = sortDesc(negativeAspectSummary, 5);

        LocalDateTime latestExecutedAt = records.stream()
                .map(ReviewAiAnalysisRecord::analyzedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new ReviewAiDashboardSummary(
                reviewAiJsonlStorageService.getOutputFile().toString(),
                records.size(),
                positiveCount,
                negativeCount,
                neutralCount,
                latestExecutedAt,
                sortedPositiveAspectSummary,
                sortedNegativeAspectSummary
        );
    }

    private Map<String, Long> sortDesc(Map<String, Long> input, int limit) {
        Map<String, Long> result = new LinkedHashMap<>();
        input.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record ReviewAiDashboardSummary(
            String exportFile,
            long totalReviews,
            long positiveCount,
            long negativeCount,
            long neutralCount,
            LocalDateTime latestExecutedAt,
            Map<String, Long> positiveAspectSummary,
            Map<String, Long> negativeAspectSummary
    ) {
    }
}