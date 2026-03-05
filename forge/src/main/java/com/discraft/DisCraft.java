package com.discraft;

import com.discraft.bridge.ChatBridgeManager;
import com.discraft.config.DisCraftConfig;
import com.discraft.gui.ConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(DisCraft.MOD_ID)
public class DisCraft {

    public static final String MOD_ID = "discraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static DisCraftConfig CONFIG;
    public static ChatBridgeManager BRIDGE;
    public static KeyMapping CONFIG_KEY;

    public DisCraft() {
        CONFIG = DisCraftConfig.load();
        BRIDGE = new ChatBridgeManager();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::registerKeyMappings);

        MinecraftForge.EVENT_BUS.register(new ForgeEvents());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            BRIDGE.shutdown();
            CONFIG.save();
            LOGGER.info("[DisCraft] 已关闭");
        }, "discraft-shutdown"));

        LOGGER.info("[DisCraft] 初始化完成");
    }

    private void clientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (mc, parent) -> new ConfigScreen(parent)
            )
        );
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        CONFIG_KEY = new KeyMapping(
                "key.discraft.open_config",
                GLFW.GLFW_KEY_G,
                "category.discraft"
        );
        event.register(CONFIG_KEY);
    }

    /**
     * 解析当前游戏上下文为唯一字符串 key。
     *   单人存档: "world:存档名"
     *   多人服务器: "server:ip"
     */
    public static String resolveContext(Minecraft mc) {
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            String levelName = mc.getSingleplayerServer().getWorldData().getLevelName();
            return "world:" + sanitize(levelName);
        }
        if (mc.getCurrentServer() != null) {
            String addr = mc.getCurrentServer().ip;
            if (addr.endsWith(":25565")) addr = addr.substring(0, addr.length() - 6);
            return "server:" + addr.toLowerCase();
        }
        return "unknown";
    }

    private static String sanitize(String s) {
        return s == null ? "unknown" : s.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
    }
}
