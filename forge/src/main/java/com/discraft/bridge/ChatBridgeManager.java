package com.discraft.bridge;

import com.discraft.DisCraft;
import com.discraft.config.WorldMapping;
import com.discraft.discord.DiscordIpc;
import com.discraft.discord.WebhookClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatBridgeManager {

    private final WebhookClient webhookClient = new WebhookClient();
    private final DiscordIpc discordIpc = new DiscordIpc();

    private String currentContext = null;
    private WorldMapping currentMapping = null;

    // ---- 上下文切换 ----

    public void onContextSwitch(String context, Minecraft client) {
        leaveVoice(client);

        this.currentContext = context;
        this.currentMapping = DisCraft.CONFIG.getMapping(context);

        if (currentMapping == null || !currentMapping.enabled) {
            showInfo(client, "§7[DisCraft] 当前存档/服务器未绑定 Discord 频道，按 G 打开设置");
            return;
        }

        String name = currentMapping.displayName.isBlank() ? context : currentMapping.displayName;
        showInfo(client, "§a[DisCraft] §r已连接到 Discord 频道：§b" + name);

        if (currentMapping.showJoinLeave && !currentMapping.webhookUrl.isBlank()) {
            String player = client.getUser().getName();
            webhookClient.sendEmbedMessage(currentMapping.webhookUrl,
                    player, player + " 加入了游戏", null, WebhookClient.colorJoin());
        }

        String voiceChannelId = currentMapping.voiceChannelId;
        if (!voiceChannelId.isBlank()) {
            Thread t = new Thread(() -> {
                if (discordIpc.ensureConnected()) {
                    discordIpc.selectVoiceChannel(voiceChannelId, client);
                } else if (discordIpc.isConnected()) {
                    showInfo(client, "§7[DisCraft] 语音自动跳转需要先授权 Discord，请按 G 打开设置");
                } else {
                    showInfo(client, "§7[DisCraft] Discord 未运行，语音自动跳转不可用");
                }
            }, "discraft-voice-join");
            t.setDaemon(true);
            t.start();
        }
    }

    public void onDisconnect(Minecraft client) {
        if (currentMapping != null
                && currentMapping.showJoinLeave
                && !currentMapping.webhookUrl.isBlank()) {
            String player = client.getUser().getName();
            webhookClient.sendEmbedMessage(currentMapping.webhookUrl,
                    player, player + " 离开了游戏", null, WebhookClient.colorLeave());
        }
        leaveVoice(client);
        currentMapping = null;
        currentContext = null;
    }

    // ---- 消息转发 ----

    public void onPlayerChat(String message, Minecraft client) {
        if (!isActive()) return;
        if (!currentMapping.sendToDiscord) return;
        if (currentMapping.webhookUrl.isBlank()) return;

        String player = client.getUser().getName();
        webhookClient.sendChatMessage(currentMapping.webhookUrl, player, message);
    }

    public void onPlayerDeath(String deathMessage) {
        if (!isActive()) return;
        if (!currentMapping.showDeaths) return;
        if (currentMapping.webhookUrl.isBlank()) return;
        String[] parts = deathMessage.split(" ", 2);
        String deadPlayer = parts[0];
        webhookClient.sendEmbedMessage(currentMapping.webhookUrl,
                deadPlayer, deathMessage, null, WebhookClient.colorDeath());
    }

    public void onAdvancement(Component title, Component description) {
        if (!isActive()) return;
        if (!currentMapping.showAdvancements) return;
        if (currentMapping.webhookUrl.isBlank()) return;
        Minecraft client = Minecraft.getInstance();
        String player = (client != null) ? client.getUser().getName() : "";
        webhookClient.sendEmbedMessage(currentMapping.webhookUrl,
                player, title.getString(), description.getString(), WebhookClient.colorAdv());
    }

    // ---- IPC 控制 ----

    public void startIpcAuth(Minecraft client) {
        Thread t = new Thread(() -> discordIpc.startAuth(client), "discraft-auth");
        t.setDaemon(true);
        t.start();
    }

    private void leaveVoice(Minecraft client) {
        if (!discordIpc.isConnected()) return;
        Thread t = new Thread(() -> discordIpc.selectVoiceChannel(null, client), "discraft-voice-leave");
        t.setDaemon(true);
        t.start();
    }

    // ---- 状态查询 ----

    public boolean isActive() {
        return currentMapping != null && currentMapping.enabled;
    }

    public String getCurrentContext() {
        return currentContext;
    }

    public WorldMapping getCurrentMapping() {
        return currentMapping;
    }

    public boolean isVoiceConnected() {
        return discordIpc.isConnected();
    }

    // ---- 生命周期 ----

    public void shutdown() {
        discordIpc.disconnect();
        webhookClient.shutdown();
    }

    // ---- 工具方法 ----

    private void showInfo(Minecraft client, String message) {
        client.execute(() -> {
            if (client.gui != null) {
                client.gui.getChat().addMessage(Component.literal(message));
            }
        });
    }
}
