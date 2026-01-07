package com.example.websockets.Config;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleWebSocketHandler extends TextWebSocketHandler {

    // userId -> WebSocketSession
    private static final Map<String, WebSocketSession> users =
            new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserId(session);
        users.put(userId, session);

        System.out.println("User connected: " + userId);
        System.out.println("Online users: " + users.keySet());
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {

        String senderId = getUserId(session);

        /*
        Expected message format:
        receiverId:actual message
        Example:
        john:Hello John
        */
        String payload = message.getPayload();
        String[] parts = payload.split(":", 2);

        if (parts.length != 2) {
            session.sendMessage(
                new TextMessage("Invalid format. Use receiverId:message")
            );
            return;
        }

        String receiverId = parts[0];
        String content = parts[1];

        WebSocketSession receiverSession = users.get(receiverId);

        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(
                new TextMessage("From " + senderId + ": " + content)
            );
        } else {
            session.sendMessage(
                new TextMessage("User " + receiverId + " is offline")
            );
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        users.remove(userId);

        System.out.println("User disconnected: " + userId);
        System.out.println("Online users: " + users.keySet());
    }

    private String getUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return "anonymous";
        }

        for (String param : uri.getQuery().split("&")) {
            if (param.startsWith("userId=")) {
                return param.substring("userId=".length());
            }
        }
        return "anonymous";
    }
}
