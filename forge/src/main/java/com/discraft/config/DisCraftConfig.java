package com.discraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DisCraftConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("discraft");
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("discraft");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String discordAccessToken = "";
    public Map<String, WorldMapping> mappings = new HashMap<>();

    public static DisCraftConfig load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE);
                DisCraftConfig cfg = GSON.fromJson(json, DisCraftConfig.class);
                if (cfg != null) {
                    if (cfg.mappings == null) cfg.mappings = new HashMap<>();
                    if (cfg.discordAccessToken == null) cfg.discordAccessToken = "";
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
