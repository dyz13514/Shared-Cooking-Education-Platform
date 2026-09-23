package com.example.teachingplatform.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@Service
public class BigModelChatService {

    private static final Logger log = LoggerFactory.getLogger(BigModelChatService.class);
    private static final String ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private final boolean aiEnabled;
    private final String apiKey;
    private final String model;
    private final String visionModel;
    private final boolean thinkingEnabled;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public BigModelChatService(@Value("${app.ai.enabled:false}") boolean aiEnabled,
                               @Value("${app.ai.api-key:}") String apiKey,
                               @Value("${app.ai.model:glm-4.7}") String model,
                               @Value("${app.ai.vision-model:glm-4.6v}") String visionModel,
                               @Value("${app.ai.thinking.enabled:false}") boolean thinkingEnabled) {
        this.aiEnabled = aiEnabled;
        this.apiKey = apiKey;
        this.model = model;
        this.visionModel = visionModel;
        this.thinkingEnabled = thinkingEnabled;
    }

    public String chat(String systemPrompt, List<Message> messages, String fallbackReply) {
        if (!isConfigured()) {
            return fallbackReply;
        }
        try {
            String body = buildBody(systemPrompt, messages, false, 2048, 0.7);
            return request(body, fallbackReply);
        } catch (Exception ex) {
            log.error("AI 普通问答请求构造失败", ex);
            return fallbackReply;
        }
    }

    public String streamChat(String systemPrompt, List<Message> messages, Consumer<String> onDelta, String fallbackReply) {
        if (!isConfigured()) {
            return fallbackReply;
        }
        try {
            String body = buildBody(systemPrompt, messages, true, 4096, 0.6);
            return streamRequest(body, onDelta, fallbackReply);
        } catch (Exception ex) {
            log.error("AI 流式问答请求构造失败", ex);
            return fallbackReply;
        }
    }

    public String visionChat(String prompt, String imageUrlOrBase64, String fallbackReply) {
        if (!isConfigured() || imageUrlOrBase64 == null || imageUrlOrBase64.isBlank()) {
            return fallbackReply;
        }
        try {
            String body = "{"
                    + "\"model\":" + json(visionModel) + ","
                    + "\"messages\":[{\"role\":\"user\",\"content\":["
                    + "{\"type\":\"image_url\",\"image_url\":{\"url\":" + json(imageUrlOrBase64) + "}},"
                    + "{\"type\":\"text\",\"text\":" + json(prompt) + "}"
                    + "]}],"
                    + thinkingJson()
                    + "\"max_tokens\":2048,"
                    + "\"temperature\":0.5"
                    + "}";
            return request(body, fallbackReply);
        } catch (Exception ex) {
            log.error("AI 看图请求失败", ex);
            return fallbackReply;
        }
    }

    private String streamRequest(String body, Consumer<String> onDelta, String fallbackReply) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = new String(response.body().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            log.warn("AI 流式接口返回错误状态: {}，body: {}", response.statusCode(), errorBody);
            return fallbackReply;
        }
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(response.body(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            StringBuilder full = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isBlank() || "[DONE]".equals(data)) {
                    continue;
                }
                String delta = extractDeltaContent(data);
                if (delta != null && !delta.isEmpty()) {
                    full.append(delta);
                    if (onDelta != null) {
                        onDelta.accept(delta);
                    }
                }
            }
            String reply = full.toString();
            if (reply.isBlank()) {
                log.warn("AI 流式接口未解析到任何回复内容");
                return fallbackReply;
            }
            return reply;
        }
    }

    private String request(String body, String fallbackReply) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("AI 普通接口返回错误状态: {}，body: {}", response.statusCode(), response.body());
            return fallbackReply;
        }
        String reply = extractMessageContent(response.body());
        if (reply == null || reply.isBlank()) {
            log.warn("AI 普通接口未解析到回复内容，body: {}", response.body());
            return fallbackReply;
        }
        return reply.trim();
    }

    private String buildBody(String systemPrompt, List<Message> messages, boolean stream, int maxTokens, double temperature) {
        StringBuilder messageJson = new StringBuilder("[");
        boolean hasMessage = false;
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messageJson.append("{\"role\":\"system\",\"content\":").append(json(systemPrompt)).append("}");
            hasMessage = true;
        }
        for (Message message : messages) {
            if (hasMessage) {
                messageJson.append(',');
            }
            messageJson.append("{\"role\":").append(json(message.role()))
                    .append(",\"content\":").append(json(message.content()))
                    .append("}");
            hasMessage = true;
        }
        messageJson.append(']');
        return "{"
                + "\"model\":" + json(model) + ","
                + "\"messages\":" + messageJson + ","
                + (stream ? "\"stream\":true," : "")
                + thinkingJson()
                + "\"max_tokens\":" + maxTokens + ","
                + "\"temperature\":" + temperature
                + "}";
    }

    private String thinkingJson() {
        return thinkingEnabled ? "\"thinking\":{\"type\":\"enabled\"}," : "";
    }

    private String json(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private String extractDeltaContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        String deltaMarker = "\"delta\"";
        int deltaPos = responseBody.indexOf(deltaMarker);
        if (deltaPos < 0) {
            return null;
        }
        String marker = "\"content\"";
        int key = responseBody.indexOf(marker, deltaPos);
        if (key < 0) {
            return null;
        }
        return extractJsonStringValue(responseBody, key + marker.length());
    }

    private String extractMessageContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        String marker = "\"content\"";
        int key = responseBody.indexOf(marker);
        if (key < 0) {
            return null;
        }
        return extractJsonStringValue(responseBody, key + marker.length());
    }

    private String extractJsonStringValue(String responseBody, int searchFrom) {
        int colon = responseBody.indexOf(':', searchFrom);
        if (colon < 0) {
            return null;
        }
        int start = responseBody.indexOf('"', colon + 1);
        if (start < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        for (int i = start + 1; i < responseBody.length(); i++) {
            char c = responseBody.charAt(i);
            if (escaping) {
                switch (c) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 4 < responseBody.length()) {
                            String hex = responseBody.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append("\\u").append(hex);
                                i += 4;
                            }
                        }
                    }
                    default -> sb.append(c);
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    private boolean isConfigured() {
        return aiEnabled && apiKey != null && !apiKey.isBlank();
    }

    public void ensureConfiguredOrThrow() {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI 服务未启用或未配置 BIGMODEL_API_KEY");
        }
    }

    public record Message(String role, String content) {}
}
