package com.sonnybell.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * WebChat class to handle HTTP requests for the web chat feature.
 * It implements HttpHandler to process GET and POST requests for chat messages.
 */
public class WebChat implements HttpHandler {
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_OK = 200;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int UNKNOWN_CONTENT_LENGTH = -1;
    private static final java.util.Set<String> WEB_USERS = ConcurrentHashMap.newKeySet();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("/api/webchat/messages".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                handleGetMessages(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePostMessage(exchange);
            } else {
                exchange.sendResponseHeaders(HTTP_METHOD_NOT_ALLOWED, UNKNOWN_CONTENT_LENGTH);
            }
        } else if ("/api/webchat/login".equals(path)) {
            if ("POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
            } else {
                exchange.sendResponseHeaders(HTTP_METHOD_NOT_ALLOWED, UNKNOWN_CONTENT_LENGTH);
            }
        } else if ("/api/webchat/webusers".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                handleGetWebUsers(exchange);
            } else {
                exchange.sendResponseHeaders(HTTP_METHOD_NOT_ALLOWED, UNKNOWN_CONTENT_LENGTH);
            }
        } else if ("/api/webchat/logout".equals(path)) {
            if ("POST".equalsIgnoreCase(method)) {
                handleLogout(exchange);
            } else {
                exchange.sendResponseHeaders(HTTP_METHOD_NOT_ALLOWED, UNKNOWN_CONTENT_LENGTH);
            }
        } else {
            exchange.sendResponseHeaders(HTTP_NOT_FOUND, UNKNOWN_CONTENT_LENGTH);
        }
    }

    private void handleGetMessages(HttpExchange exchange) throws IOException {
        List<String> messages = ChatHistory.getMessageHistory();
        JSONArray arr = new JSONArray();
        for (String msg : messages) {
            arr.put(msg);
        }
        byte[] resp = arr.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(HTTP_OK, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    private void handlePostMessage(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject obj = new JSONObject(body);
        String user = obj.optString("user", "webuser");
        String message = obj.optString("message", "");
        if (!message.isBlank()) {
            String formatted = user + ": " + message;
            ChatHistory.addMessageToHistory(formatted);
            // Broadcast to all connected socket clients
            ClientHandler.broadcastMessageToAll(formatted);
        }
        exchange.sendResponseHeaders(HTTP_NO_CONTENT, UNKNOWN_CONTENT_LENGTH);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject obj = new JSONObject(body);
        String username = obj.optString("username", "");
        String password = obj.optString("password", "");
        boolean valid = password.equals(Server.getServerPass()) && !username.isBlank();

        if (valid) {
            if (WEB_USERS.add(username)) { // Only announce if newly added
                String joinMsg = "SERVER: " + username + " has joined the chat!";
                ChatHistory.addMessageToHistory(joinMsg);
                System.out.println("[WebChat] Web user logged in: " + username);
            }
        }

        JSONObject resp = new JSONObject();
        resp.put("valid", valid);
        byte[] respBytes = resp.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(HTTP_OK, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }

    private void handleGetWebUsers(HttpExchange exchange) throws IOException {
        JSONArray arr = new JSONArray(WEB_USERS);
        byte[] resp = arr.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(HTTP_OK, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject obj = new JSONObject(body);
        String username = obj.optString("username", "");
        boolean removed = WEB_USERS.remove(username);
        if (removed) {
            String leaveMsg = "SERVER: " + username + " has left the chat.";
            ChatHistory.addMessageToHistory(leaveMsg);
            System.out.println("[WebChat] Web user logged out: " + username);
        }
        JSONObject resp = new JSONObject();
        resp.put("removed", removed);
        byte[] respBytes = resp.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(HTTP_OK, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }
    // TODO: implement message logging.
    // TODO: implement web users tracking.
    // TODO: implement quit quitting.
}
