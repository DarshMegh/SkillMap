package skillmap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ApiHandler implements HttpHandler {

    // ✅ Now using local Ollama — no API key needed
    private final OllamaClient client = new OllamaClient();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORS headers — allow all origins for local dev
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        try {
            // Read request body
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String goal = extractJsonField(body, "goal");
            String level = extractJsonField(body, "level");
            String timeframe = extractJsonField(body, "timeframe");

            if (goal == null || goal.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"goal field is required\"}");
                return;
            }

            // Call Anthropic API
            String roadmap = client.generateRoadmap(goal, level, timeframe);
            String jsonResponse = "{\"roadmap\":" + toJsonString(roadmap) + "}";
            sendJson(exchange, 200, jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Minimal JSON field extractor — no external library needed
    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx + key.length());
        if (colon == -1) return null;
        int start = json.indexOf('"', colon + 1);
        if (start == -1) return null;
        int end = start + 1;
        while (end < json.length() && json.charAt(end) != '"') {
            if (json.charAt(end) == '\\') end++; // skip escaped char
            end++;
        }
        return json.substring(start + 1, end);
    }

    private String toJsonString(String value) {
        if (value == null) return "null";
        return "\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}