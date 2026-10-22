package com.hotel.system.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record ReviewAiInferenceResult(
        @JsonProperty("raw_text")
        String rawText,

        @JsonProperty("normalized_text")
        String normalizedText,

        @JsonProperty("direct_sentiment_label")
        String directSentimentLabel,

        @JsonProperty("direct_sentiment_score")
        Double directSentimentScore,

        @JsonProperty("direct_sentiment_probabilities")
        Map<String, Double> directSentimentProbabilities,

        @JsonProperty("ml_aspect_labels")
        List<String> mlAspectLabels,

        @JsonProperty("lexicon_aspect_labels")
        List<String> lexiconAspectLabels,

        @JsonProperty("aspect_labels")
        List<String> aspectLabels,

        @JsonProperty("ml_aspect_score")
        Double mlAspectScore,

        @JsonProperty("lexicon_aspect_score")
        Double lexiconAspectScore,

        @JsonProperty("aspect_score")
        Double aspectScore,

        @JsonProperty("lexicon_clause_score")
        Double lexiconClauseScore,

        @JsonProperty("final_score")
        Double finalScore,

        @JsonProperty("final_label")
        String finalLabel
) {
}