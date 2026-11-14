package com.example.capture_the_moment.websocket.chat;

import com.example.capture_the_moment.services.chat.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionProjects = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long projectId = projectId(session);
        String username = username(session);
        try {
            chatService.requireMembership(projectId, username);
        } catch (RuntimeException exception) {
            session.close(new CloseStatus(HttpStatus.FORBIDDEN.value(), "Project membership required"));
            return;
        }
        ChatService.ChatHistory history = chatService.history(projectId, username);
        rooms.computeIfAbsent(projectId, ignored -> new CopyOnWriteArraySet<>()).add(session);
        sessionProjects.put(session.getId(), projectId);
        send(session, Map.of("type", "history", "messages", history.messages()));
        broadcastPresence(projectId);
        broadcast(projectId, Map.of("type", "system", "message", username + " joined the chat"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws IOException {
        Long projectId = sessionProjects.get(session.getId());
        if (projectId == null || !session.isOpen()) {
            return;
        }
        try {
            JsonNode payload = objectMapper.readTree(textMessage.getPayload());
            String content = payload.path("content").asText(null);
            ChatService.ChatMessage message = chatService.saveMessage(projectId, username(session), content);
            broadcast(projectId, Map.of("type", "message", "message", message));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            send(session, Map.of("type", "error", "message", "Invalid chat message."));
        } catch (RuntimeException exception) {
            send(session, Map.of("type", "error", "message",
                    exception.getMessage() == null ? "Message could not be sent." : exception.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long projectId = sessionProjects.remove(session.getId());
        if (projectId == null) {
            return;
        }
        CopyOnWriteArraySet<WebSocketSession> members = rooms.get(projectId);
        if (members != null) {
            members.remove(session);
            if (members.isEmpty()) {
                rooms.remove(projectId, members);
            } else {
                broadcastPresence(projectId);
                broadcast(projectId, Map.of("type", "system",
                        "message", username(session) + " left the chat"));
            }
        }
    }

    private void broadcastPresence(Long projectId) {
        List<String> usernames = new ArrayList<>();
        for (WebSocketSession member : rooms.getOrDefault(projectId, new CopyOnWriteArraySet<>())) {
            String username = username(member);
            if (!usernames.contains(username)) {
                usernames.add(username);
            }
        }
        broadcast(projectId, Map.of("type", "presence", "users", usernames));
    }

    private void broadcast(Long projectId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            for (WebSocketSession member : rooms.getOrDefault(projectId, new CopyOnWriteArraySet<>())) {
                send(member, json);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode chat event.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not deliver chat event.", exception);
        }
    }

    private void send(WebSocketSession session, Object payload) throws IOException {
        send(session, objectMapper.writeValueAsString(payload));
    }

    private void send(WebSocketSession session, String payload) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(payload));
        }
    }

    private Long projectId(WebSocketSession session) {
        return Long.valueOf((String) session.getAttributes().get("projectId"));
    }

    private String username(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        return principal == null ? "" : principal.getName();
    }
}
