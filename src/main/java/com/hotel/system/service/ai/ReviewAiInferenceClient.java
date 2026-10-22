package com.hotel.system.service.ai;

import com.hotel.system.dto.ai.ReviewAiInferenceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewAiInferenceClient {

    private static final Logger log = LoggerFactory.getLogger(ReviewAiInferenceClient.class);
    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String modelDir;
    private final boolean useBkai;

    public ReviewAiInferenceClient(
            @Value("${ai.review.api.url:http://localhost:5000/api/analyze}") String apiUrl,
            @Value("${ai.review.python.model-dir:artifacts/models}") String modelDir,
            @Value("${ai.review.python.use-bkai:false}") boolean useBkai) {
        this.restTemplate = new RestTemplate();
        this.apiUrl = apiUrl;
        this.modelDir = modelDir;
        this.useBkai = useBkai;
    }

    public ReviewAiInferenceResult analyze(String reviewText) {
        String sanitized = reviewText == null ? "" : reviewText.trim();
        if (sanitized.isBlank()) {
            return new ReviewAiInferenceResult(
                    sanitized, sanitized, "neutral", 0.0,
                    java.util.Map.of(), List.of(), List.of(), List.of(),
                    0.0, 0.0, 0.0, 0.0, 0.0, "neutral"
            );
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", sanitized);
            requestBody.put("model_dir", modelDir);
            requestBody.put("use_bkai", useBkai);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Bắn request sang server Python và tự động map kết quả JSON trả về vào đối tượng ReviewAiInferenceResult
            return restTemplate.postForObject(apiUrl, request, ReviewAiInferenceResult.class);
        } catch (Exception e) {
            log.error("Lỗi khi gọi AI API tại {}: {}", apiUrl, e.getMessage());
            throw new IllegalStateException("Không thể kết nối đến Python AI API.", e);
        }
    }
}