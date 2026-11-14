package com.example.capture_the_moment.controllers.chat;

import com.example.capture_the_moment.services.chat.ChatService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/project/{projectId}/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/history")
    public ChatService.ChatHistory history(@PathVariable Long projectId,
                                           @AuthenticationPrincipal UserDetails user) {
        return chatService.history(projectId, user.getUsername());
    }
}
