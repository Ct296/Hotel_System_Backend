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

    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> body,
                                                    HttpSession session) {

        String message = body.getOrDefault("message", "");

        // Lấy User từ JWT thay vì Session cũ
        Users currentUser = getCurrentUser();

        // Gọi service xử lý chat (vẫn dùng session để giữ lịch sử hội thoại cho Gemini)
        String reply = chatService.reply(message, currentUser, session);

        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);

        return ResponseEntity.ok(result);
    }
}