package com.discraft.bridge;

import com.discraft.DisCraft;
import com.discraft.config.WorldMapping;
import com.discraft.discord.GatewayClient;
import com.discraft.discord.WebhookClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 聊天桥接核心管理器。
 * 负责：
 *  - 根据当前游戏上下文（存档/服务器）切换 Discord 频道
 *  - 将游戏内聊天转发到 Discord Webhook
 *  - 从 Discord Gateway 接收消息并注入游戏内聊天
 */
public class ChatBridgeManager {

    private final WebhookClient webhookClient = new WebhookClient();
    private final GatewayClient gatewayClient = new GatewayClient();

    private String currentContext = null;
    private WorldMapping currentMapping = null;

    // ---- 上下文切换 ----

    /**
     * 当玩家进入存档或连接到服务器时调用。
     * @param context "world:存档名" 或 "server:ip"
     */
    public void onContextSwitch(String context, MinecraftClient client) {
        // 断开旧连接
        gatewayClient.disconnect();

        this.currentContext = context;
        this.currentMapping = DisCraft.CONFIG.getMapping(context);

        if (currentMapping == null || !currentMapping.enabled) {
            showInfo(client, "§7[DisCraft] 当前存档/服务器未绑定 Discord 频道，按 G 打开设置");
            return;
        }

        // 建立新的 Gateway 连接（用于接收 Discord 消息）
        if (currentMapping.receiveFromDiscord
                && !currentMapping.channelId.isBlank()
                && DisCraft.CONFIG.hasBotToken()) {
            gatewayClient.connect(DisCraft.CONFIG.botToken, currentMapping.channelId);
        }

        // 显示状态
        String name = currentMapping.displayName.isBlank() ? context : currentMapping.displayName;
        showInfo(client, "§a[DisCraft] §r已连接到 Discord 频道：§b" + name);

        // 发送加入通知
        if (currentMapping.showJoinLeave && !currentMapping.webhookUrl.isBlank()) {
            String player = client.getSession().getUsername();
            webhookClient.sendSystemMessage(currentMapping.webhookUrl,
                    "🟢 **" + player + "** 加入了游戏");
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
        gatewayClient.disconnect();
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

    public boolean isGatewayConnected() {
        return gatewayClient.isConnected();
    }

    // ---- 生命周期 ----

    public void shutdown() {
        gatewayClient.disconnect();
        webhookClient.shutdown();
    }

    // ---- 工具方法 ----

    private void showInfo(MinecraftClient client, String message) {
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(message));
        }
    }
}
