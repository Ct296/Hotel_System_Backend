package com.hotel.system.controller;

import com.hotel.system.entity.Users;
import com.hotel.system.repository.UsersRepository;
import com.hotel.system.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final UsersRepository usersRepository;

    public ChatController(ChatService chatService, UsersRepository usersRepository) {
        this.chatService = chatService;
        this.usersRepository = usersRepository;
    }

    // Helper: Lấy User hiện tại từ JWT Security Context
    private Users getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            Optional<Users> userOpt = usersRepository.findByEmail(userDetails.getUsername());
            return userOpt.orElse(null);
        }
        return null;
    }

    // API Chatbot: Nhận tin nhắn từ khách hàng và trả lời từ Gemini AI
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> body,
                                                    HttpSession session) {
        String message = body.getOrDefault("message", "");
        // Lấy User từ JWT thay vì Session cũ
        Users currentUser = getCurrentUser();
        
        if (currentUser != null && currentUser.getRole() != com.hotel.system.entity.enums.Role.CUSTOMER) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("reply", "Xin lỗi, chatbot hỗ trợ AI chỉ dành cho khách hàng.");
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(errorResult);
        }
        String reply = chatService.reply(message, currentUser, session);

        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);

        return ResponseEntity.ok(result);
    }

    
}