package com.discraft.discord;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebhookClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("discraft");
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "discraft-webhook");
        t.setDaemon(true);
        return t;
    });

    private static final int COLOR_JOIN  = 0x57F287;
    private static final int COLOR_LEAVE = 0xED4245;
    private static final int COLOR_DEATH = 0x2C2F33;
    private static final int COLOR_ADV   = 0xFEE75C;

    private boolean isBlankUrl(String url) {
        return url == null || url.isBlank();
    }

    public void sendChatMessage(String webhookUrl, String playerName, String content) {
        if (isBlankUrl(webhookUrl)) return;
        executor.submit(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("username", playerName + " ⛏");
            body.addProperty("content", escapeMarkdown(content));
            body.addProperty("avatar_url", "https://minotar.net/avatar/" + playerName + "/64");
            post(webhookUrl, body.toString());
        });
    }

    public void sendEmbedMessage(String webhookUrl, String playerName, String title, String description, int color) {
        if (isBlankUrl(webhookUrl)) return;
        executor.submit(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"embeds\":[{");
            sb.append("\"author\":{\"name\":\"").append(escapeJson(title)).append("\"");
            if (playerName != null && !playerName.isBlank()) {
                sb.append(",\"icon_url\":\"https://minotar.net/avatar/").append(playerName).append("/64\"");
            }
            sb.append("}");
            if (description != null) {
                sb.append(",\"description\":\"").append(escapeJson(description)).append("\"");
            }
            sb.append(",\"color\":").append(color);
            sb.append("}]}");
            post(webhookUrl, sb.toString());
        });
    }

    public static int colorJoin()  { return COLOR_JOIN; }
    public static int colorLeave() { return COLOR_LEAVE; }
    public static int colorDeath() { return COLOR_DEATH; }
    public static int colorAdv()   { return COLOR_ADV; }

    public void sendSystemMessage(String webhookUrl, String content) {
        if (isBlankUrl(webhookUrl)) return;
        executor.submit(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("username", "DisCraft");
            body.addProperty("content", content);
            body.addProperty("avatar_url", "https://mc.maxcdn.gg/items/grass_block.png");
            post(webhookUrl, body.toString());
        });
    }

    private void post(String url, String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                LOGGER.warn("[DisCraft] Webhook 发送失败 HTTP {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            LOGGER.error("[DisCraft] Webhook 发送异常", e);
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }

    private String escapeMarkdown(String text) {
        return text.replace("\\", "\\\\")
                   .replace("*", "\\*")
                   .replace("_", "\\_")
                   .replace("`", "\\`")
                   .replace("~", "\\~");
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
