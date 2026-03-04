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

/**
 * 通过 Discord Webhook 发送消息。
 * 无需 Bot Token，只需 Webhook URL。
 * 消息在独立线程发送，不阻塞游戏主线程。
 */
public class WebhookClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("discraft");
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "discraft-webhook");
        t.setDaemon(true);
        return t;
    });

    /**
     * 以玩家身份（名字 + MC头像）发送聊天消息。
     */
    private boolean isBlankUrl(String url) {
        return url == null || url.isBlank();
    }

    public void sendChatMessage(String webhookUrl, String playerName, String content) {
        if (isBlankUrl(webhookUrl)) return;
        executor.submit(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("username", playerName + " ⛏");
            body.addProperty("content", escapeMarkdown(content));
            // Minotar 提供 MC 皮肤头像
            body.addProperty("avatar_url", "https://minotar.net/avatar/" + playerName + "/64");
            post(webhookUrl, body.toString());
        });
    }

    /**
     * 以 DisCraft 系统身份发送事件通知（加入/离开/死亡等）。
     */
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

    /** 简单转义 Discord Markdown 特殊字符，防止意外格式化 */
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
