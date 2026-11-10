package com.hotel.system.controller;

import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.security.SecurityUtils;
import com.hotel.system.service.ChatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody Map<String, String> body,
                                    HttpSession session) {
        String message = body.getOrDefault("message", "");
        Users currentUser = securityUtils.getCurrentUser();
         
        if (currentUser != null && currentUser.getRole() != Role.CUSTOMER) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("reply", "Xin lỗi, chatbot hỗ trợ AI chỉ dành cho khách hàng.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResult);
        }
        
        String reply = chatService.reply(message, currentUser, session);
        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);

        return ResponseEntity.ok(result);
    }
}