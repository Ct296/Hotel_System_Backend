package com.hotel.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.system.entity.AppliedPeriod;
import com.hotel.system.entity.Bill;
import com.hotel.system.entity.Payment;
import com.hotel.system.entity.PriceRate;
import com.hotel.system.entity.Rental;
import com.hotel.system.entity.RentalDetail;
import com.hotel.system.entity.RoomType;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.BillType;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.entity.enums.ServiceStatus;
import com.hotel.system.repository.AppliedPeriodRepository;
import com.hotel.system.repository.BillRepository;
import com.hotel.system.repository.PaymentRepository;
import com.hotel.system.repository.RentalRepository;
import com.hotel.system.repository.RoomTypeRepository;
import com.hotel.system.repository.ServiceRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");

    private final ServiceRepository serviceRepository;
    private final RentalRepository rentalRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final AppliedPeriodRepository appliedPeriodRepository;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    private final Map<String, List<Long>> requestTimestampsByUser = new ConcurrentHashMap<>();

    public ChatService(ServiceRepository serviceRepository,
                       RentalRepository rentalRepository,
                       BillRepository billRepository,
                       PaymentRepository paymentRepository,
                       RoomTypeRepository roomTypeRepository,
                       AppliedPeriodRepository appliedPeriodRepository) {
        this.serviceRepository = serviceRepository;
        this.rentalRepository = rentalRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.appliedPeriodRepository = appliedPeriodRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String reply(String message, Users currentUser, HttpSession session) {
        String normalizedMessage = normalizeMessage(message);

        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return "Bạn hãy nhập câu hỏi cụ thể, ví dụ: giá phòng, dịch vụ, chính sách cọc hoặc booking gần nhất của tôi.";
        }

        if (normalizedMessage.length() > MAX_MESSAGE_LENGTH) {
            return "Câu hỏi hơi dài. Bạn vui lòng rút gọn trong khoảng 500 ký tự để mình hỗ trợ chính xác hơn nhé.";
        }

        if (!isAllowed(normalizedMessage)) {
            return """
                    Mình chỉ hỗ trợ cung cấp thông tin và hướng dẫn trong hệ thống khách sạn.
                    Mình không thể thao tác thay bạn như đặt phòng, hủy booking, sửa dữ liệu hay thanh toán hộ.
                    Nếu bạn cần thực hiện thao tác, vui lòng dùng chức năng trên website hoặc liên hệ lễ tân.
                    """;
        }

        String rateLimitKey = resolveRateLimitKey(currentUser, session);
        if (!tryAcquire(rateLimitKey)) {
            return "Bạn gửi câu hỏi hơi nhanh. Vui lòng chờ một chút rồi thử lại nhé.";
        }

        String ruleBased = tryRuleBasedReply(normalizedMessage, currentUser);
        if (ruleBased != null && !ruleBased.isBlank()) {
            return ruleBased;
        }

        try {
            String context = buildContext(currentUser);
            String aiReply = callGemini(normalizedMessage, context);
            if (aiReply != null && !aiReply.isBlank()) {
                return cleanupAiReply(aiReply);
            }
        } catch (Exception ignored) {
        }

        return fallbackReply(normalizedMessage, currentUser);
    }

    private String normalizeMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.trim().replaceAll("\\s+", " ");
    }

    private boolean isAllowed(String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        List<String> blockedPatterns = List.of(
                "hãy đặt phòng", "đặt giúp", "book giúp", "hãy hủy", "hủy giúp",
                "thanh toán giúp", "trả tiền giúp", "xóa booking", "sửa booking",
                "thay đổi booking", "làm giúp tôi", "thực hiện giúp",
                "đặt hộ", "hủy hộ", "book hộ", "thanh toán hộ"
        );

        for (String pattern : blockedPatterns) {
            if (lower.contains(pattern)) {
                return false;
            }
        }
        return true;
    }

    private String resolveRateLimitKey(Users currentUser, HttpSession session) {
        if (currentUser != null && currentUser.getId() != null) {
            return "USER_" + currentUser.getId();
        }
        return "SESSION_" + session.getId();
    }

    private boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        List<Long> timestamps = requestTimestampsByUser.computeIfAbsent(
                key, k -> Collections.synchronizedList(new ArrayList<>())
        );

        synchronized (timestamps) {
            timestamps.removeIf(ts -> now - ts > RATE_LIMIT_WINDOW_MS);
            if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
                return false;
            }
            timestamps.add(now);
            return true;
        }
    }

    private String buildContext(Users currentUser) {
        StringBuilder context = new StringBuilder();

        context.append("THÔNG TIN KHÁCH SẠN ITHotel\n");
        context.append("- Đây là chatbot hỗ trợ thông tin cho website khách sạn.\n");
        context.append("- Không thực hiện thao tác thay người dùng.\n");
        context.append("- Ưu tiên trả lời ngắn gọn, rõ ràng, đúng dữ liệu nội bộ.\n\n");

        appendRoomPrices(context);
        appendServices(context);
        appendUserSpecificInfo(context, currentUser);

        return context.toString();
    }

    private void appendRoomPrices(StringBuilder context) {
        List<RoomType> roomTypes = roomTypeRepository.findAllByOrderByNameAsc();
        if (roomTypes == null || roomTypes.isEmpty()) {
            return;
        }

        context.append("BẢNG GIÁ LOẠI PHÒNG:\n");
        for (RoomType roomType : roomTypes) {
            if (roomType == null) continue;

            double basePrice = roomType.getBasePrice() == null ? 0.0 : roomType.getBasePrice();

            context.append("- ")
                    .append(safe(roomType.getName()))
                    .append(": giá cơ bản ")
                    .append(formatMoney(basePrice))
                    .append("/giờ");

            if ((roomType.getAdultMax() + roomType.getChildrenMax()) > 0) {
                context.append(", sức chứa ").append((roomType.getAdultMax() + roomType.getChildrenMax())).append(" người");
            }

            if (roomType.getDepositPercent() != null) {
                context.append(", cọc ").append(trimTrailingZeros(roomType.getDepositPercent())).append("%");
            }

            if (roomType.getDescription() != null && !roomType.getDescription().isBlank()) {
                context.append(", mô tả: ").append(roomType.getDescription().trim());
            }

            List<AppliedPeriod> periods = roomType.getId() == null
                    ? List.of()
                    : appliedPeriodRepository.findByRoomTypeIdOrderByStartDateDesc(roomType.getId());

            if (periods != null && !periods.isEmpty()) {
                AppliedPeriod latestPeriod = periods.stream()
                        .filter(p -> p.getPriceRate() != null && p.getPriceRate().getSurchargeAmount() != null)
                        .findFirst()
                        .orElse(null);

                if (latestPeriod != null) {
                    PriceRate priceRate = latestPeriod.getPriceRate();
                    double surcharge = priceRate.getSurchargeAmount() == null ? 0.0 : priceRate.getSurchargeAmount();
                    double effective = basePrice + surcharge;

                    context.append(", có kỳ áp dụng gần đây: ")
                            .append(safe(priceRate.getEventName()))
                            .append(" -> ")
                            .append(formatMoney(effective))
                            .append("/giờ trong khoảng ")
                            .append(formatDateTime(latestPeriod.getStartDate()))
                            .append(" đến ")
                            .append(formatDateTime(latestPeriod.getEndDate()));
                }
            }

            context.append("\n");
        }
        context.append("\n");
    }

    private void appendServices(StringBuilder context) {
        List<com.hotel.system.entity.Service> services =
                serviceRepository.findByStatusOrderByCreateDateDesc(ServiceStatus.ACTIVE);

        if (services == null || services.isEmpty()) {
            return;
        }

        context.append("DỊCH VỤ ĐANG HOẠT ĐỘNG:\n");
        int count = 0;
        for (com.hotel.system.entity.Service service : services) {
            if (service == null) continue;

            context.append("- ")
                    .append(safe(service.getName()))
                    .append(": ")
                    .append(formatMoney(service.getBasePrice() != null ? service.getBasePrice() : 0.0))
                    .append("/")
                    .append(safe(service.getUnit()));

            if (service.getDescription() != null && !service.getDescription().isBlank()) {
                context.append(", ").append(service.getDescription().trim());
            }

            context.append("\n");
            count++;
            if (count >= 10) break;
        }

        context.append("\n");
    }

    private void appendUserSpecificInfo(StringBuilder context, Users currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.CUSTOMER || currentUser.getId() == null) {
            return;
        }

        try {
            List<Rental> rentals = rentalRepository.findByCustomerId(currentUser.getId());
            if (rentals == null || rentals.isEmpty()) {
                return;
            }

            rentals.sort((a, b) -> {
                LocalDateTime da = a != null ? a.getRentDate() : null;
                LocalDateTime db = b != null ? b.getRentDate() : null;
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return db.compareTo(da);
            });

            Rental latestRental = rentals.get(0);
            RentalDetail firstDetail = (latestRental.getRentalDetails() != null && !latestRental.getRentalDetails().isEmpty()) 
                                        ? latestRental.getRentalDetails().get(0) : null;

            context.append("BOOKING/LƯỢT THUÊ GẦN NHẤT CỦA KHÁCH HIỆN TẠI:\n");
            context.append("- Mã: ").append(safe(latestRental.getId())).append("\n");
            
            if (firstDetail != null) {
                context.append("- Phòng: ")
                        .append(firstDetail.getRoom() != null ? safe(firstDetail.getRoom().getName()) : "N/A")
                        .append("\n");
                context.append("- Trạng thái: ")
                        .append(latestRental.getStatus() != null ? safe(latestRental.getStatus().getDisplayName()) : "N/A")
                        .append("\n");
                context.append("- Giờ nhận phòng: ").append(formatDateTime(firstDetail.getCheckinDate())).append("\n");
                context.append("- Số giờ thuê: ")
                        .append(firstDetail.getLengthOfStay() != null ? firstDetail.getLengthOfStay() : 0)
                        .append("\n\n");
            }

            appendBillingInfo(context, latestRental.getId());
        } catch (Exception ignored) {
        }
    }

    private void appendBillingInfo(StringBuilder context, String rentalId) {
        if (rentalId == null || rentalId.isBlank()) {
            return;
        }

        List<Bill> bills = billRepository.findByRentalIdOrderByCreateDateDesc(rentalId);
        if (bills != null && !bills.isEmpty()) {
            Bill latestBill = bills.get(0);
            context.append("HÓA ĐƠN GẦN NHẤT:\n");
            context.append("- Mã hóa đơn: ").append(safe(latestBill.getId())).append("\n");
            context.append("- Loại: ")
                    .append(latestBill.getType() != null ? safe(latestBill.getType().getDisplayName()) : "N/A")
                    .append("\n");
            context.append("- Tổng tiền: ")
                    .append(formatMoney(latestBill.getTotalAmount() != null ? latestBill.getTotalAmount() : 0.0))
                    .append("\n\n");
        }

        List<Payment> payments = paymentRepository.findByBillRentalIdOrderByDateDesc(rentalId);
        if (payments != null && !payments.isEmpty()) {
            Payment latestPayment = payments.get(0);
            context.append("THANH TOÁN GẦN NHẤT:\n");
            context.append("- Mã giao dịch: ").append(safe(latestPayment.getTransaction())).append("\n");
            context.append("- Phương thức: ")
                    .append(latestPayment.getMethod() != null ? safe(latestPayment.getMethod().getDisplayName()) : "N/A")
                    .append("\n");
            context.append("- Thời gian: ").append(formatDateTime(latestPayment.getDate())).append("\n\n");
        }
    }

    private String callGemini(String userMessage, String context) throws Exception {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return null;
        }

        String url = geminiApiUrl + "/" + geminiModel + ":generateContent?key=" + geminiApiKey;

        Map<String, Object> body = new LinkedHashMap<>();

        Map<String, Object> systemInstruction = new LinkedHashMap<>();
        systemInstruction.put("parts", List.of(Map.of(
                "text",
                """
                Bạn là ITHotel Assistant, chatbot hỗ trợ thông tin cho hệ thống khách sạn.
                Nguyên tắc:
                - Chỉ trả lời các câu hỏi liên quan đến khách sạn, phòng, giá phòng, booking, tiền cọc, thanh toán, dịch vụ, chính sách.
                - Không giả vờ thực hiện hành động thay người dùng.
                - Không bịa ra dữ liệu không có trong context.
                - Nếu người dùng hỏi giá phòng, hãy ưu tiên dùng BẢNG GIÁ LOẠI PHÒNG trong context.
                - Nếu câu hỏi là "phòng giá 200k", "phòng 300k", hãy trả lời theo loại phòng có mức giá gần nhất hoặc phù hợp từ dữ liệu nội bộ.
                - Nếu không có dữ liệu chính xác, nói rõ là hiện bạn chỉ có giá cơ bản hoặc cần người dùng chọn thời gian để kiểm tra chính xác hơn.
                - Trả lời tự nhiên, ngắn gọn, bằng tiếng Việt, có ích cho người dùng.
                
                NGỮ CẢNH NỘI BỘ:
                """ + context
        )));
        body.put("systemInstruction", systemInstruction);

        Map<String, Object> userContent = new LinkedHashMap<>();
        userContent.put("parts", List.of(Map.of("text", userMessage)));
        body.put("contents", List.of(userContent));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.25);
        generationConfig.put("topP", 0.9);
        generationConfig.put("maxOutputTokens", 512);
        body.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return null;
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return null;
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            JsonNode textNode = part.get("text");
            if (textNode != null && !textNode.isNull()) {
                text.append(textNode.asText());
            }
        }

        String result = text.toString().trim();
        return result.isBlank() ? null : result;
    }

    private String tryRuleBasedReply(String message, Users currentUser) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "phòng giá", "giá phòng", "loại phòng", "phòng bao nhiêu", "rẻ nhất", "giá rẻ")) {
            return buildRoomPriceReply(lower);
        }

        if (containsAny(lower, "dịch vụ", "service")) {
            List<com.hotel.system.entity.Service> services =
                    serviceRepository.findByStatusOrderByCreateDateDesc(ServiceStatus.ACTIVE);

            if (services == null || services.isEmpty()) {
                return "Hiện tại khách sạn chưa có dịch vụ nào đang hoạt động trong hệ thống.";
            }

            StringBuilder sb = new StringBuilder("Hiện khách sạn đang có các dịch vụ sau:\n");
            int count = 0;
            for (com.hotel.system.entity.Service service : services) {
                sb.append("- ")
                        .append(safe(service.getName()))
                        .append(": ")
                        .append(formatMoney(service.getBasePrice() != null ? service.getBasePrice() : 0.0))
                        .append("/")
                        .append(safe(service.getUnit()));

                if (service.getDescription() != null && !service.getDescription().isBlank()) {
                    sb.append(" - ").append(service.getDescription().trim());
                }
                sb.append("\n");

                count++;
                if (count >= 8) break;
            }
            return sb.toString().trim();
        }

        if (containsAny(lower, "chính sách", "quy định", "hủy phòng", "tiền cọc")) {
            return "Bạn có thể hỏi cụ thể hơn như: chính sách tiền cọc, hủy booking, hoặc quy định lưu trú để mình tóm tắt đúng nội dung cần xem.";
        }

        if (currentUser != null && currentUser.getRole() == Role.CUSTOMER
                && containsAny(lower, "booking", "đặt phòng", "đơn gần nhất", "gần nhất")) {
            List<Rental> rentals = rentalRepository.findByCustomerId(currentUser.getId());
            if (rentals == null || rentals.isEmpty()) {
                return "Bạn hiện chưa có booking hoặc lượt thuê phòng nào trong hệ thống.";
            }

            rentals.sort((a, b) -> {
                LocalDateTime da = a != null ? a.getRentDate() : null;
                LocalDateTime db = b != null ? b.getRentDate() : null;
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return db.compareTo(da);
            });

            Rental latest = rentals.get(0);
            RentalDetail firstDetail = (latest != null && latest.getRentalDetails() != null && !latest.getRentalDetails().isEmpty()) 
                                        ? latest.getRentalDetails().get(0) : null;
                                        
            if (firstDetail != null) {
                return "Booking/lượt thuê gần nhất của bạn là mã "
                        + safe(latest.getId())
                        + ", trạng thái "
                        + (latest.getStatus() != null ? safe(latest.getStatus().getDisplayName()) : "N/A")
                        + ", nhận phòng lúc "
                        + formatDateTime(firstDetail.getCheckinDate()) + ".";
            }
            return "Lỗi hiển thị dữ liệu booking gần nhất.";
        }

        if (currentUser != null && currentUser.getRole() == Role.CUSTOMER
                && containsAny(lower, "thanh toán", "tiền cọc", "payment", "đã cọc chưa", "đã thanh toán chưa")) {
            List<Rental> rentals = rentalRepository.findByCustomerId(currentUser.getId());
            if (rentals == null || rentals.isEmpty()) {
                return "Bạn hiện chưa có booking hoặc lượt thuê nào để kiểm tra thanh toán.";
            }

            rentals.sort((a, b) -> {
                LocalDateTime da = a != null ? a.getRentDate() : null;
                LocalDateTime db = b != null ? b.getRentDate() : null;
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return db.compareTo(da);
            });

            Rental latest = rentals.get(0);
            boolean hasDepositPayment = paymentRepository.existsByBillRentalIdAndBillType(latest.getId(), BillType.DEPOSIT);

            if (hasDepositPayment) {
                return "Booking/lượt thuê gần nhất của bạn đã có ghi nhận thanh toán tiền cọc.";
            }
            return "Booking/lượt thuê gần nhất của bạn hiện chưa có ghi nhận thanh toán tiền cọc.";
        }

        return null;
    }

    private String buildRoomPriceReply(String lower) {
        List<RoomType> roomTypes = roomTypeRepository.findAllByOrderByNameAsc();
        if (roomTypes == null || roomTypes.isEmpty()) {
            return "Hiện hệ thống chưa có dữ liệu loại phòng để mình báo giá.";
        }

        Integer requestedPrice = extractRequestedPrice(lower);

        if (requestedPrice != null) {
            RoomType closest = null;
            double closestPrice = 0.0;
            double minDiff = Double.MAX_VALUE;

            for (RoomType roomType : roomTypes) {
                double price = roomType.getBasePrice() == null ? 0.0 : roomType.getBasePrice();
                double diff = Math.abs(price - requestedPrice);
                if (diff < minDiff) {
                    minDiff = diff;
                    closest = roomType;
                    closestPrice = price;
                }
            }

            if (closest != null) {
                return "Nếu bạn đang tìm khoảng "
                        + formatMoney(requestedPrice.doubleValue())
                        + "/giờ thì loại gần nhất hiện tại là "
                        + safe(closest.getName())
                        + " với giá cơ bản "
                        + formatMoney(closestPrice)
                        + "/giờ"
                        + ((closest.getAdultMax() + closest.getChildrenMax()) > 0 ? ", sức chứa " + (closest.getAdultMax() + closest.getChildrenMax()) + " người." : ".");
            }
        }

        RoomType cheapest = roomTypes.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparing(rt -> rt.getBasePrice() == null ? Double.MAX_VALUE : rt.getBasePrice()))
                .orElse(null);

        StringBuilder sb = new StringBuilder("Giá cơ bản các loại phòng hiện tại:\n");
        for (RoomType roomType : roomTypes) {
            double basePrice = roomType.getBasePrice() == null ? 0.0 : roomType.getBasePrice();
            sb.append("- ")
                    .append(safe(roomType.getName()))
                    .append(": ")
                    .append(formatMoney(basePrice))
                    .append("/giờ");

            if ((roomType.getAdultMax() + roomType.getChildrenMax()) > 0) {
                sb.append(", sức chứa ").append((roomType.getAdultMax() + roomType.getChildrenMax())).append(" người");
            }
            sb.append("\n");
        }

        if (cheapest != null) {
            double cheapestPrice = cheapest.getBasePrice() == null ? 0.0 : cheapest.getBasePrice();
            sb.append("\nLoại phòng có giá cơ bản thấp nhất hiện tại là ")
                    .append(safe(cheapest.getName()))
                    .append(" với mức ")
                    .append(formatMoney(cheapestPrice))
                    .append("/giờ.");
        }

        sb.append("\nLưu ý: giá thực tế có thể thay đổi theo kỳ áp dụng giá tại thời điểm bạn chọn nhận phòng.");
        return sb.toString().trim();
    }

    private Integer extractRequestedPrice(String lower) {
        String normalized = lower.replace(".", "").replace(",", "").replace(" ", "");
        StringBuilder digits = new StringBuilder();

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }

        if (digits.isEmpty()) {
            return null;
        }

        try {
            int value = Integer.parseInt(digits.toString());

            if (normalized.contains("k") && value < 10000) {
                value = value * 1000;
            }

            return value;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String fallbackReply(String message, Users currentUser) {
        return """
                Mình có thể hỗ trợ các nội dung như:
                - Giá các loại phòng và mức giá gần đúng bạn đang tìm
                - Chính sách đặt phòng, hủy booking, tiền cọc
                - Dịch vụ khách sạn đang hoạt động
                - Booking/lượt thuê gần nhất của bạn
                - Tình trạng thanh toán gần đây

                Bạn có thể hỏi ví dụ:
                - phòng giá 200k
                - dịch vụ khách sạn có gì
                - booking gần nhất của tôi
                - tôi đã thanh toán cọc chưa
                """;
    }

    private String cleanupAiReply(String text) {
        if (text == null) {
            return null;
        }
        return text.trim()
                .replaceAll("\\n{3,}", "\n\n")
                .replace("**", "");
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "N/A";
    }

    private String formatMoney(double value) {
        return MONEY_FORMAT.format(value) + " đ";
    }

    private String trimTrailingZeros(Double value) {
        if (value == null) {
            return "0";
        }
        if (Math.floor(value) == value) {
            return String.valueOf(value.intValue());
        }
        return String.valueOf(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }
}