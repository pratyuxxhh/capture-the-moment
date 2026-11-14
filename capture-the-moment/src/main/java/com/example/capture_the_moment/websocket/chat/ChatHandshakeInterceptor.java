package com.example.capture_the_moment.websocket.chat;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriTemplate;

import java.util.Map;

public class ChatHandshakeInterceptor implements HandshakeInterceptor {
    private final UriTemplate uriTemplate = new UriTemplate("/ws/chat/{projectId}");

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Map<String, String> variables = uriTemplate.match(request.getURI().getPath());
        attributes.put("projectId", variables.get("projectId"));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
