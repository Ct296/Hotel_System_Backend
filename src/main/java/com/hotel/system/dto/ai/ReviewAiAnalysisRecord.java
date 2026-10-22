package com.hotel.system.dto.ai;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ReviewAiAnalysisRecord(
        String reviewId,
        String customerId,
        String customerName,
        Integer reviewRate,
        String reviewText,
        LocalDateTime reviewUpdateDate,
        LocalDateTime analyzedAt,
        String rawText,
        String normalizedText,
        String directSentimentLabel,
        Double directSentimentScore,
        Map<String, Double> directSentimentProbabilities,
        List<String> mlAspectLabels,
        List<String> lexiconAspectLabels,
        List<String> aspectLabels,
        Double mlAspectScore,
        Double lexiconAspectScore,
        Double aspectScore,
        Double lexiconClauseScore,
        Double finalScore,
        String finalLabel,
        String primaryAspect
) {
}
