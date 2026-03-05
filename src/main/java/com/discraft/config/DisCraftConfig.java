package com.discraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DisCraftConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("discraft");
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("discraft");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ---- 字段 ----

    /** Discord 应用 Client ID（在 Discord Developer Portal 获取） */
    public String discordClientId = "";

    /** Discord 应用 Client Secret（在 Discord Developer Portal 获取） */
    public String discordClientSecret = "";

    /** Discord OAuth2 access_token（通过 IPC 授权流程获取，自动保存） */
    public String discordAccessToken = "";

    /**
     * 存档/服务器上下文 -> Discord 频道映射。
     * key 示例: "world:My Survival", "server:hypixel.net"
     */
    public Map<String, WorldMapping> mappings = new HashMap<>();

    // ---- 加载 / 保存 ----

    public static DisCraftConfig load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE);
                DisCraftConfig cfg = GSON.fromJson(json, DisCraftConfig.class);
                if (cfg != null) {
                    if (cfg.mappings == null) cfg.mappings = new HashMap<>();
                    if (cfg.discordAccessToken == null) cfg.discordAccessToken = "";
                    if (cfg.discordClientId == null) cfg.discordClientId = "";
                    if (cfg.discordClientSecret == null) cfg.discordClientSecret = "";
                    return cfg;
                }
            }
        } catch (IOException e) {
            LOGGER.error("[DisCraft] 配置文件读取失败，使用默认配置", e);
        }
        return new DisCraftConfig();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(CONFIG_FILE, GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.error("[DisCraft] 配置文件保存失败", e);
        }
    }

    // ---- 辅助方法 ----

    public WorldMapping getMapping(String context) {
        return mappings.get(context);
    }

    public void setMapping(String context, WorldMapping mapping) {
        mappings.put(context, mapping);
    }

    public void removeMapping(String context) {
        mappings.remove(context);
    }
}
