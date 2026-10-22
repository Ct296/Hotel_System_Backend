package com.hotel.system.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.system.dto.ai.ReviewAiInferenceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewAiInferenceClient {

    private final ObjectMapper objectMapper;
    private final String pythonExecutable;
    private final Path aiProjectDir;
    private final String testCliScript;
    private final String modelDir;
    private final boolean useBkai;
    private final Duration timeout;
    private static final Logger log = LoggerFactory.getLogger(ReviewAiInferenceClient.class);
    public ReviewAiInferenceClient(ObjectMapper objectMapper,
                                   @Value("${ai.review.python.executable:python}") String pythonExecutable,
                                   @Value("${ai.review.python.project-dir:../Hotel_Comment_AI}") String aiProjectDir,
                                   @Value("${ai.review.python.test-cli:scripts/test_cli.py}") String testCliScript,
                                   @Value("${ai.review.python.model-dir:artifacts/models}") String modelDir,
                                   @Value("${ai.review.python.use-bkai:false}") boolean useBkai,
                                   @Value("${ai.review.python.timeout-seconds:60}") long timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.pythonExecutable = pythonExecutable;
        this.aiProjectDir = Path.of(aiProjectDir).toAbsolutePath().normalize();
        this.testCliScript = testCliScript;
        this.modelDir = modelDir;
        this.useBkai = useBkai;
        this.timeout = Duration.ofSeconds(Math.max(10, timeoutSeconds));
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

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add("-X");
        command.add("utf8");
        command.add(testCliScript);
        command.add("--text");
        command.add(sanitized);
        command.add("--model-dir");
        command.add(modelDir);
        if (useBkai) {
            command.add("--use-bkai");
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(aiProjectDir.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("PYTHONUTF8", "1");
        processBuilder.environment().put("PYTHONIOENCODING", "UTF-8");
        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeout.toSeconds(), java.util.concurrent.TimeUnit.SECONDS);
            String output = readProcessOutput(process);

            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("AI inference bi timeout sau " + timeout.toSeconds() + " giay.");
            }

            if (process.exitValue() != 0) {
                throw new IllegalStateException("AI inference tra ve loi. Output: " + output);
            }

            return objectMapper.readValue(output, ReviewAiInferenceResult.class);
        } catch (IOException e) {
            throw new IllegalStateException("Khong the goi Python AI inference.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tien trinh AI inference bi gian doan.", e);
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString().trim();
    }
}
