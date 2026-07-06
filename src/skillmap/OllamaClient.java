package skillmap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OllamaClient {

    // Change this to whatever model you have pulled: llama3, mistral, gemma2, phi3, etc.
    private static final String MODEL = "mistral";
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String generateRoadmap(String goal, String level, String timeframe) throws Exception {
        String prompt = buildPrompt(goal, level, timeframe);

        // Ollama /api/generate request body — stream:false so we get one response
        String requestBody = """
                {
                  "model": "%s",
                  "prompt": %s,
                  "stream": false,
                  "options": {
                    "temperature": 0.7,
                    "num_predict": 2048
                  }
                }
                """.formatted(MODEL, toJsonString(prompt));

        System.out.println("[Ollama] Sending request for goal: " + goal);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(3))   // local models can be slow
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Ollama error " + response.statusCode() + ": " + response.body() +
                            "\n\nMake sure Ollama is running: `ollama serve`" +
                            "\nAnd the model is pulled: `ollama pull " + MODEL + "`"
            );
        }

        return extractResponse(response.body());
    }

    private String buildPrompt(String goal, String level, String timeframe) {
        String lvl = (level == null || level.isBlank()) ? "beginner" : level;
        String tf  = (timeframe == null || timeframe.isBlank()) ? "3 months" : timeframe;

        return """
You are SkillMap AI — a world-class learning roadmap generator. Be specific and practical.

Generate a structured learning roadmap for:
Goal: %s
Current level: %s
Timeframe: %s

Use EXACTLY this format:

## 🎯 Goal Overview
2-sentence summary of what the learner will achieve.

## 📍 Where You Start vs Where You'll End
Before/after comparison in 3-4 bullet points.

## 🗺️ Phase-by-Phase Roadmap

### Phase 1: [Name] (Week X–Y)
**Focus:** What this phase covers
**Topics:**
- Topic 1
- Topic 2
- Topic 3
**Resources:**
- Resource Name (URL if known) — why useful
**Milestone:** What you can do/build after this phase

### Phase 2: [Name] (Week X–Y)
**Focus:** ...
**Topics:**
- ...
**Resources:**
- ...
**Milestone:** ...

(3 to 5 phases total)

## ⚡ Key Projects to Build
- Project 1 — one line description
- Project 2 — one line description
- Project 3 — one line description

## 🧠 Tips for Success
- Tip 1
- Tip 2
- Tip 3

## 📊 Progress Checkpoints
- Week 2: checkpoint
- Week 4: checkpoint
- Week 8: checkpoint

Be direct, specific, and encouraging. No filler text.
""".formatted(goal, lvl, tf);
    }

    // Ollama response JSON: {"model":"...","response":"...","done":true,...}
    private String extractResponse(String json) {
        String marker = "\"response\":\"";
        int start = json.indexOf(marker);
        if (start == -1) {
            // fallback — return raw if unexpected format
            System.err.println("[Ollama] Unexpected response format: " + json.substring(0, Math.min(200, json.length())));
            return "Could not parse Ollama response. Raw: " + json.substring(0, Math.min(500, json.length()));
        }
        start += marker.length();

        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default   -> sb.append(next);
                }
                i += 2;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }

        String result = sb.toString().trim();
        System.out.println("[Ollama] Response received, length: " + result.length() + " chars");
        return result;
    }

    private String toJsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}