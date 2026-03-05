package com.discraft.bridge;

import com.discraft.DisCraft;
import com.discraft.config.WorldMapping;
import com.discraft.discord.DiscordIpc;
import com.discraft.discord.WebhookClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * 聊天桥接核心管理器。
 * 负责：
 *  - 根据当前游戏上下文（存档/服务器）切换 Discord 语音频道（通过 IPC）
 *  - 将游戏内聊天、加入/离开、死亡、成就事件转发到 Discord Webhook
 */
public class ChatBridgeManager {

    private final WebhookClient webhookClient = new WebhookClient();
    private final DiscordIpc discordIpc = new DiscordIpc();

    private String currentContext = null;
    private WorldMapping currentMapping = null;

    // ---- 上下文切换 ----

    /**
     * 当玩家进入存档或连接到服务器时调用。
     * @param context "world:存档名" 或 "server:ip"
     */
    public void onContextSwitch(String context, MinecraftClient client) {
        // 离开旧语音频道
        leaveVoice(client);

        this.currentContext = context;
        this.currentMapping = DisCraft.CONFIG.getMapping(context);

        if (currentMapping == null || !currentMapping.enabled) {
            showInfo(client, "§7[DisCraft] 当前存档/服务器未绑定 Discord 频道，按 G 打开设置");
            return;
        }

        // 显示连接状态
        String name = currentMapping.displayName.isBlank() ? context : currentMapping.displayName;
        showInfo(client, "§a[DisCraft] §r已连接到 Discord 频道：§b" + name);

        // 发送加入通知
        if (currentMapping.showJoinLeave && !currentMapping.webhookUrl.isBlank()) {
            String player = client.getSession().getUsername();
            webhookClient.sendSystemMessage(currentMapping.webhookUrl,
                    "🟢 **" + player + "** 加入了游戏");
        }

        // 尝试加入语音频道（后台线程）
        String voiceChannelId = currentMapping.voiceChannelId;
        if (!voiceChannelId.isBlank()) {
            Thread t = new Thread(() -> {
                if (discordIpc.ensureConnected()) {
                    discordIpc.selectVoiceChannel(voiceChannelId, client);
                } else if (discordIpc.isConnected()) {
                    // 已连接但未认证（无 token）
                    showInfo(client, "§7[DisCraft] 语音自动跳转需要先授权 Discord，请按 G 打开设置");
                } else {
                    showInfo(client, "§7[DisCraft] Discord 未运行，语音自动跳转不可用");
                }
            }, "discraft-voice-join");
            t.setDaemon(true);
            t.start();
        }
    }

    /** 玩家断开连接时调用 */
    public void onDisconnect(MinecraftClient client) {
        if (currentMapping != null
                && currentMapping.showJoinLeave
                && !currentMapping.webhookUrl.isBlank()) {
            String player = client.getSession().getUsername();
            webhookClient.sendSystemMessage(currentMapping.webhookUrl,
                    "🔴 **" + player + "** 离开了游戏");
        }
        leaveVoice(client);
        currentMapping = null;
        currentContext = null;
    }

    // ---- 消息转发 ----

    /** 游戏内玩家发送聊天消息时调用 */
    public void onPlayerChat(String message, MinecraftClient client) {
        if (!isActive()) return;
        if (!currentMapping.sendToDiscord) return;
        if (currentMapping.webhookUrl.isBlank()) return;

        String player = client.getSession().getUsername();
        webhookClient.sendChatMessage(currentMapping.webhookUrl, player, message);
    }

    /** 玩家死亡时调用 */
    public void onPlayerDeath(String deathMessage) {
        if (!isActive()) return;
        if (!currentMapping.showDeaths) return;
        if (currentMapping.webhookUrl.isBlank()) return;
        webhookClient.sendSystemMessage(currentMapping.webhookUrl, "💀 " + deathMessage);
    }

    /** 玩家获得成就时调用（由 AdvancementToastMixin 触发） */
    public void onAdvancement(Text title, Text description) {
        if (!isActive()) return;
        if (!currentMapping.showAdvancements) return;
        if (currentMapping.webhookUrl.isBlank()) return;
        webhookClient.sendSystemMessage(currentMapping.webhookUrl,
                "🏆 **" + title.getString() + "** — " + description.getString());
    }

    // ---- IPC 控制 ----

    /** 在后台线程启动 OAuth2 授权流程（由设置界面的授权按钮触发） */
    public void startIpcAuth(MinecraftClient client) {
        Thread t = new Thread(() -> discordIpc.startAuth(client), "discraft-auth");
        t.setDaemon(true);
        t.start();
    }

    private void leaveVoice(MinecraftClient client) {
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

    private void showInfo(MinecraftClient client, String message) {
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(message));
        }
    }
}
