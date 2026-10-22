package com.hotel.system.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.system.dto.ai.ReviewAiAnalysisRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewAiJsonlStorageService {

    private final ObjectMapper objectMapper;
    private final Path outputFile;

    public ReviewAiJsonlStorageService(ObjectMapper objectMapper,
                                       @Value("${ai.review.output-file:storage/ai/review_ai_analysis.jsonl}") String outputFile) {
        this.objectMapper = objectMapper;
        this.outputFile = Path.of(outputFile).toAbsolutePath().normalize();
    }

    public Path getOutputFile() {
        return outputFile;
    }

    public Map<String, ReviewAiAnalysisRecord> loadAllByReviewId() {
        Map<String, ReviewAiAnalysisRecord> result = new LinkedHashMap<>();
        if (!Files.exists(outputFile)) {
            return result;
        }

        try {
            List<String> lines = Files.readAllLines(outputFile, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine == null ? "" : rawLine.trim();
                if (line.isBlank()) {
                    continue;
                }
                ReviewAiAnalysisRecord record = objectMapper.readValue(line, ReviewAiAnalysisRecord.class);
                result.put(record.reviewId(), record);
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Khong the doc file JSONL ket qua AI: " + outputFile, e);
        }
    }

    public void saveAll(Map<String, ReviewAiAnalysisRecord> records) {
        try {
            Files.createDirectories(outputFile.getParent());
            List<ReviewAiAnalysisRecord> sorted = records.values().stream()
                    .sorted((a, b) -> {
                        if (a.reviewUpdateDate() == null && b.reviewUpdateDate() == null) return 0;
                        if (a.reviewUpdateDate() == null) return 1;
                        if (b.reviewUpdateDate() == null) return -1;
                        return b.reviewUpdateDate().compareTo(a.reviewUpdateDate());
                    })
                    .collect(Collectors.toList());

            try (BufferedWriter writer = Files.newBufferedWriter(
                    outputFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                for (ReviewAiAnalysisRecord record : sorted) {
                    writer.write(objectMapper.writeValueAsString(record));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Khong the ghi file JSONL ket qua AI: " + outputFile, e);
        }
    }
}
