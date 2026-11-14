package com.example.capture_the_moment.websocket.kanban;

import com.example.capture_the_moment.controllers.kanban.KanbanController;
import com.example.capture_the_moment.services.kanban.KanbanServices;
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

public class KanbanWebSocketHandler extends TextWebSocketHandler {
    private final KanbanServices kanbanServices;
    private final ObjectMapper objectMapper;
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> boards = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionProjects = new ConcurrentHashMap<>();

    public KanbanWebSocketHandler(KanbanServices kanbanServices, ObjectMapper objectMapper) {
        this.kanbanServices = kanbanServices;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long projectId = projectId(session);
        String username = username(session);
        try {
            kanbanServices.requireMembership(projectId, username);
        } catch (RuntimeException exception) {
            session.close(new CloseStatus(HttpStatus.FORBIDDEN.value(), "Project membership required"));
            return;
        }
        boards.computeIfAbsent(projectId, ignored -> new CopyOnWriteArraySet<>()).add(session);
        sessionProjects.put(session.getId(), projectId);
        send(session, Map.of("type", "state", "tasks", kanbanServices.tasks(projectId, username).stream()
                .map(KanbanController.TaskResponse::from)
                .toList()));
        broadcastPresence(projectId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long projectId = sessionProjects.get(session.getId());
        if (projectId == null || !session.isOpen()) {
            return;
        }
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText();
            String username = username(session);
            if ("create".equals(type)) {
                KanbanController.TaskResponse task = KanbanController.TaskResponse.from(
                        kanbanServices.createTask(projectId, username, payload.path("title").asText(null),
                                payload.path("description").asText(""), payload.path("assigneeUsername").asText(null)));
                broadcast(projectId, Map.of("type", "task-created", "task", task));
            } else if ("status".equals(type)) {
                KanbanController.TaskResponse task = KanbanController.TaskResponse.from(
                        kanbanServices.updateStatus(projectId, payload.path("taskId").asLong(),
                                username, payload.path("status").asText()));
                broadcast(projectId, Map.of("type", "task-updated", "task", task));
            } else if ("delete".equals(type)) {
                kanbanServices.deleteTask(projectId, payload.path("taskId").asLong(), username);
                broadcast(projectId, Map.of("type", "task-deleted", "taskId", payload.path("taskId").asLong()));
            }
        } catch (RuntimeException exception) {
            send(session, Map.of("type", "error", "message",
                    exception.getMessage() == null ? "Board update failed." : exception.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long projectId = sessionProjects.remove(session.getId());
        if (projectId == null) {
            return;
        }
        CopyOnWriteArraySet<WebSocketSession> members = boards.get(projectId);
        if (members != null) {
            members.remove(session);
            if (members.isEmpty()) {
                boards.remove(projectId, members);
            } else {
                broadcastPresence(projectId);
            }
        }
    }

    private void broadcastPresence(Long projectId) {
        List<String> users = new ArrayList<>();
        for (WebSocketSession member : boards.getOrDefault(projectId, new CopyOnWriteArraySet<>())) {
            if (!users.contains(username(member))) {
                users.add(username(member));
            }
        }
        broadcast(projectId, Map.of("type", "presence", "users", kanbanServices.onlineUsers(projectId, users)));
    }

    private void broadcast(Long projectId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            for (WebSocketSession member : boards.getOrDefault(projectId, new CopyOnWriteArraySet<>())) {
                send(member, json);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not deliver board event.", exception);
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
