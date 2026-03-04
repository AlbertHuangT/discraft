package com.discraft;

import com.discraft.bridge.ChatBridgeManager;
import com.discraft.config.DisCraftConfig;
import com.discraft.gui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisCraft implements ClientModInitializer {

    public static final String MOD_ID = "discraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static DisCraftConfig CONFIG;
    public static ChatBridgeManager BRIDGE;

    /** 按 G 打开 DisCraft 设置界面 */
    public static KeyBinding CONFIG_KEY;

    @Override
    public void onInitializeClient() {
        CONFIG = DisCraftConfig.load();
        BRIDGE = new ChatBridgeManager();

        // 注册快捷键：G
        CONFIG_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.discraft.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.discraft"
        ));

        // 进入存档/服务器时切换频道上下文
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String context = resolveContext(client);
            LOGGER.info("[DisCraft] 上下文切换: {}", context);
            BRIDGE.onContextSwitch(context, client);
        });

        // 断开连接时
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BRIDGE.onDisconnect(client);
        });

        // 聊天消息拦截由 ChatScreenMixin 完成

        // 每 tick 检测快捷键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CONFIG_KEY.wasPressed()) {
                client.setScreen(new ConfigScreen(client.currentScreen));
            }
        });

        // 游戏关闭时保存配置并断开连接
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            BRIDGE.shutdown();
            CONFIG.save();
            LOGGER.info("[DisCraft] 已关闭");
        });

        LOGGER.info("[DisCraft] 初始化完成");
    }

    /**
     * 解析当前游戏上下文为唯一字符串 key。
     *   单人存档: "world:存档名"
     *   多人服务器: "server:ip" 或 "server:ip:port"
     */
    public static String resolveContext(MinecraftClient client) {
        if (client.isIntegratedServerRunning() && client.getServer() != null) {
            // 单人/局域网存档
            String levelName = client.getServer().getSaveProperties().getLevelName();
            return "world:" + sanitize(levelName);
        }
        if (client.getCurrentServerEntry() != null) {
            // 多人服务器，去除默认端口
            String addr = client.getCurrentServerEntry().address;
            if (addr.endsWith(":25565")) addr = addr.substring(0, addr.length() - 6);
            return "server:" + addr.toLowerCase();
        }
        return "unknown";
    }

    /** 去除不能用作文件 key 的特殊字符 */
    private static String sanitize(String s) {
        return s == null ? "unknown" : s.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
    }
}
