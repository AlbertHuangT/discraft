package com.discraft.discord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Discord Gateway WebSocket 客户端。
 * 连接到 Discord 实时事件流，监听目标频道的新消息并转发到游戏内聊天。
 *
 * 需要：Bot Token + 在 Developer Portal 启用 MESSAGE_CONTENT 权限意图
 */
public class GatewayClient implements WebSocket.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger("discraft");
    private static final String GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json";

    // Discord Gateway Intents: GUILDS(1) | GUILD_MESSAGES(512) | MESSAGE_CONTENT(32768)
    private static final int INTENTS = 1 | 512 | 32768;

    private volatile WebSocket webSocket;
    private ScheduledExecutorService heartbeatScheduler;
    private final StringBuilder messageBuffer = new StringBuilder();

    private String botToken;
    private String targetChannelId;
    private volatile int lastSequence = -1;
    private volatile boolean running = false;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private String selfBotId = null; // 避免回显自己发的消息

    // ---- 公开 API ----

    public synchronized void connect(String token, String channelId) {
        if (running) disconnect();
        this.botToken = token;
        this.targetChannelId = channelId;
        this.running = true;
        this.lastSequence = -1;
        this.selfBotId = null;

        HttpClient client = HttpClient.newHttpClient();
        LOGGER.info("[DisCraft] 正在连接 Discord Gateway，监听频道 {}", channelId);
        client.newWebSocketBuilder()
                .buildAsync(URI.create(GATEWAY_URL), this)
                .whenComplete((ws, err) -> {
                    if (err != null) {
                        LOGGER.error("[DisCraft] Gateway 连接失败", err);
                    } else {
                        this.webSocket = ws;
                    }
                });
    }

    public synchronized void disconnect() {
        running = false;
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
            heartbeatScheduler = null;
        }
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "disconnect");
            } catch (Exception ignored) {}
            webSocket = null;
        }
    }

    public boolean isConnected() {
        return running && webSocket != null;
    }

    // ---- WebSocket.Listener 实现 ----

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        messageBuffer.append(data);
        if (last) {
            handlePayload(messageBuffer.toString());
            messageBuffer.setLength(0);
        }
        ws.request(1);
        return null;
    }

    @Override
    public void onError(WebSocket ws, Throwable error) {
        LOGGER.error("[DisCraft] Gateway WebSocket 错误", error);
        if (running) scheduleReconnect();
    }

    @Override
    public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
        LOGGER.info("[DisCraft] Gateway 连接关闭 code={} reason={}", statusCode, reason);
        if (running) scheduleReconnect();
        return null;
    }

    // ---- 内部逻辑 ----

    private void handlePayload(String json) {
        try {
            JsonObject payload = JsonParser.parseString(json).getAsJsonObject();
            int op = payload.get("op").getAsInt();

            // 更新序号
            if (payload.has("s") && !payload.get("s").isJsonNull()) {
                lastSequence = payload.get("s").getAsInt();
            }

            switch (op) {
                case 10 -> { // Hello
                    int interval = payload.getAsJsonObject("d").get("heartbeat_interval").getAsInt();
                    startHeartbeat(interval);
                    sendIdentify();
                }
                case 11 -> { /* Heartbeat ACK，忽略 */ }
                case 0 -> { // Dispatch
                    String event = payload.get("t").getAsString();
                    if ("READY".equals(event)) {
                        JsonObject user = payload.getAsJsonObject("d").getAsJsonObject("user");
                        selfBotId = user.get("id").getAsString();
                        LOGGER.info("[DisCraft] Gateway 就绪，Bot ID: {}", selfBotId);
                    } else if ("MESSAGE_CREATE".equals(event)) {
                        handleMessageCreate(payload.getAsJsonObject("d"));
                    }
                }
                case 9 -> { // Invalid Session
                    LOGGER.warn("[DisCraft] Session 无效，重新 Identify");
                    sendIdentify();
                }
            }
        } catch (Exception e) {
            LOGGER.error("[DisCraft] 解析 Gateway 消息失败: {}", json, e);
        }
    }

    private void startHeartbeat(int intervalMs) {
        if (heartbeatScheduler != null) heartbeatScheduler.shutdownNow();
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "discraft-heartbeat");
            t.setDaemon(true);
            return t;
        });
        // 先发送一次，再按 interval 周期发送
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeat, 0, intervalMs, TimeUnit.MILLISECONDS
        );
    }

    private void sendHeartbeat() {
        if (webSocket == null) return;
        JsonObject payload = new JsonObject();
        payload.addProperty("op", 1);
        if (lastSequence >= 0) {
            payload.addProperty("d", lastSequence);
        } else {
            payload.add("d", com.google.gson.JsonNull.INSTANCE);
        }
        webSocket.sendText(payload.toString(), true);
    }

    private void sendIdentify() {
        if (webSocket == null) return;
        JsonObject properties = new JsonObject();
        properties.addProperty("os", System.getProperty("os.name", "unknown").toLowerCase());
        properties.addProperty("browser", "discraft");
        properties.addProperty("device", "discraft");

        JsonObject d = new JsonObject();
        d.addProperty("token", botToken);
        d.addProperty("intents", INTENTS);
        d.add("properties", properties);

        JsonObject payload = new JsonObject();
        payload.addProperty("op", 2);
        payload.add("d", d);
        webSocket.sendText(payload.toString(), true);
    }

    private void handleMessageCreate(JsonObject data) {
        try {
            // 过滤：只处理目标频道
            String channelId = data.get("channel_id").getAsString();
            if (!channelId.equals(targetChannelId)) return;

            JsonObject author = data.getAsJsonObject("author");

            // 过滤：忽略自己发的消息（避免回显）
            String authorId = author.get("id").getAsString();
            if (authorId.equals(selfBotId)) return;

            // 过滤：忽略其他 Bot
            if (author.has("bot") && author.get("bot").getAsBoolean()) return;

            String username = author.get("username").getAsString();
            JsonElement contentEl = data.get("content");
            String content = (contentEl != null && !contentEl.isJsonNull()) ? contentEl.getAsString() : "";
            if (content.isBlank()) return; // 忽略纯附件消息

            // 截断过长内容
            if (content.length() > 256) content = content.substring(0, 253) + "...";

            final String finalContent = content;
            final String finalUsername = username;

            // 必须在主线程更新 UI
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.inGameHud == null) return;

                MutableText prefix = Text.literal("[Discord] ").setStyle(Style.EMPTY.withColor(0x5865F2));
                MutableText name = Text.literal(finalUsername).setStyle(Style.EMPTY.withFormatting(Formatting.AQUA));
                MutableText separator = Text.literal(": ").setStyle(Style.EMPTY.withFormatting(Formatting.GRAY));
                MutableText message = Text.literal(finalContent).setStyle(Style.EMPTY.withFormatting(Formatting.WHITE));

                client.inGameHud.getChatHud().addMessage(prefix.append(name).append(separator).append(message));
            });

        } catch (Exception e) {
            LOGGER.error("[DisCraft] 处理 MESSAGE_CREATE 失败", e);
        }
    }

    private void scheduleReconnect() {
        if (!reconnecting.compareAndSet(false, true)) return;
        Thread reconnectThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                if (running) {
                    LOGGER.info("[DisCraft] 尝试重连 Discord Gateway...");
                    connect(botToken, targetChannelId);
                }
            } catch (InterruptedException ignored) {
            } finally {
                reconnecting.set(false);
            }
        }, "discraft-reconnect");
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }
}
