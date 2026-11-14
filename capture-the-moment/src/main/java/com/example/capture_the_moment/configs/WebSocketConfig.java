package com.example.capture_the_moment.configs;

import com.example.capture_the_moment.websocket.chat.ChatHandshakeInterceptor;
import com.example.capture_the_moment.websocket.chat.ChatWebSocketHandler;
import com.example.capture_the_moment.websocket.kanban.KanbanHandshakeInterceptor;
import com.example.capture_the_moment.websocket.kanban.KanbanWebSocketHandler;
import com.example.capture_the_moment.services.chat.ChatService;
import com.example.capture_the_moment.services.kanban.KanbanServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatService chatService;
    private final KanbanServices kanbanServices;
    private final ObjectMapper objectMapper;

    public WebSocketConfig(ChatService chatService, KanbanServices kanbanServices, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.kanbanServices = kanbanServices;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(chatService, objectMapper), "/ws/chat/{projectId}")
                .addInterceptors(new ChatHandshakeInterceptor());
        registry.addHandler(new KanbanWebSocketHandler(kanbanServices, objectMapper), "/ws/kanban/{projectId}")
                .addInterceptors(new KanbanHandshakeInterceptor());
    }
}
