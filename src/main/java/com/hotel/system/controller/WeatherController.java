package com.hotel.system.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/weather")
public class WeatherController {

    private static final String HOTEL_LAT = "10.0452";
    private static final String HOTEL_LON = "105.7469";
    private static final String HOTEL_LOCATION = "Cần Thơ";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/current", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCurrentWeather() {
        try {
            String url = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + URLEncoder.encode(HOTEL_LAT, StandardCharsets.UTF_8)
                    + "&longitude=" + URLEncoder.encode(HOTEL_LON, StandardCharsets.UTF_8)
                    + "&current=" + URLEncoder.encode("temperature_2m,weather_code,is_day", StandardCharsets.UTF_8)
                    + "&temperature_unit=" + URLEncoder.encode("celsius", StandardCharsets.UTF_8)
                    + "&timezone=" + URLEncoder.encode("Asia/Ho_Chi_Minh", StandardCharsets.UTF_8);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return buildErrorResponse(HttpStatus.BAD_GATEWAY, "Không thể lấy dữ liệu thời tiết.");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode current = root.path("current");

            if (current.isMissingNode() || current.isNull()) {
                return buildErrorResponse(HttpStatus.BAD_GATEWAY, "Không có dữ liệu thời tiết hiện tại.");
            }

            double temperature = current.path("temperature_2m").asDouble(Double.NaN);
            int weatherCode = current.path("weather_code").asInt(-1);
            boolean isDay = current.path("is_day").asInt(1) == 1;

            if (Double.isNaN(temperature) || weatherCode < 0) {
                return buildErrorResponse(HttpStatus.BAD_GATEWAY, "Dữ liệu thời tiết không hợp lệ.");
            }

            Map<String, Object> weatherInfo = mapWeather(weatherCode, isDay);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("location", HOTEL_LOCATION);
            body.put("temperature", Math.round(temperature));
            body.put("description", weatherInfo.get("description"));
            body.put("iconText", weatherInfo.get("iconText"));
            body.put("isDay", isDay);

            return ResponseEntity.ok(body);

        } catch (IOException e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi đọc dữ liệu thời tiết.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Yêu cầu thời tiết bị gián đoạn.");
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tải thời tiết lúc này.");
        }
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("temperature", null);
        body.put("description", "Khác");
        body.put("iconText", "❓");
        body.put("isDay", true);
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> mapWeather(int weatherCode, boolean isDay) {
        Map<String, Object> result = new LinkedHashMap<>();

        String description;
        String iconText;

        switch (weatherCode) {
            case 0:
                description = isDay ? "Trời quang" : "Trời quang";
                iconText = isDay ? "☀️" : "🌙";
                break;
            case 1:
                description = "Ít mây";
                iconText = isDay ? "🌤️" : "🌙";
                break;
            case 2:
                description = "Mây rải rác";
                iconText = isDay ? "⛅" : "☁️";
                break;
            case 3:
                description = "Nhiều mây";
                iconText = "☁️";
                break;
            case 45:
            case 48:
                description = "Sương mù";
                iconText = "🌫️";
                break;
            case 51:
            case 53:
            case 55:
                description = "Mưa phùn";
                iconText = "🌦️";
                break;
            case 56:
            case 57:
                description = "Mưa phùn lạnh";
                iconText = "🌧️";
                break;
            case 61:
            case 63:
            case 65:
                description = "Mưa";
                iconText = "🌧️";
                break;
            case 66:
            case 67:
                description = "Mưa lạnh";
                iconText = "🌧️";
                break;
            case 71:
            case 73:
            case 75:
                description = "Tuyết";
                iconText = "❄️";
                break;
            case 77:
                description = "Hạt tuyết";
                iconText = "🌨️";
                break;
            case 80:
            case 81:
            case 82:
                description = "Mưa rào";
                iconText = "🌦️";
                break;
            case 85:
            case 86:
                description = "Mưa tuyết";
                iconText = "🌨️";
                break;
            case 95:
                description = "Dông";
                iconText = "⛈️";
                break;
            case 96:
            case 99:
                description = "Dông mạnh";
                iconText = "⛈️";
                break;
            default:
                description = "Khác";
                iconText = "❓";
                break;
        }

        result.put("description", description);
        result.put("iconText", iconText);
        return result;
    }
}