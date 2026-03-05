package com.discraft.gui;

import com.discraft.DisCraft;
import com.discraft.config.WorldMapping;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigScreen extends Screen {

    private final Screen parent;

    private int scrollOffset = 0;
    private static final int ENTRIES_PER_PAGE = 3;
    private List<Map.Entry<String, WorldMapping>> mappingEntries = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        super(Component.translatable("gui.discraft.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshMappingEntries();

        int centerX = this.width / 2;

        addRenderableWidget(new Button(centerX - 100, 40, 200, 20,
                Component.translatable("gui.discraft.config.authorize"), btn -> {
            btn.active = false;
            DisCraft.BRIDGE.startIpcAuth(this.minecraft);
            Thread t = new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                this.minecraft.execute(() -> btn.active = true);
            });
            t.setDaemon(true);
            t.start();
        }));

        String ctx = DisCraft.BRIDGE.getCurrentContext();
        Component ctxLabel = (ctx != null)
                ? Component.translatable("gui.discraft.config.edit_current").append(shortenContext(ctx))
                : Component.translatable("gui.discraft.config.not_in_game");
        addRenderableWidget(new Button(centerX - 100, 64, 200, 20,
                ctxLabel, btn -> {
            if (ctx != null) this.minecraft.setScreen(new MappingEditScreen(this, ctx));
        }));

        addRenderableWidget(new Button(centerX - 110, 92, 20, 20,
                Component.literal("◀"), btn -> {
            if (scrollOffset > 0) {
                scrollOffset--;
                this.init(this.minecraft, this.width, this.height);
            }
        }));

        addRenderableWidget(new Button(centerX + 90, 92, 20, 20,
                Component.literal("▶"), btn -> {
            int maxOffset = Math.max(0, mappingEntries.size() - ENTRIES_PER_PAGE);
            if (scrollOffset < maxOffset) {
                scrollOffset++;
                this.init(this.minecraft, this.width, this.height);
            }
        }));

        int startY = 122;
        int rowHeight = 24;
        for (int i = 0; i < ENTRIES_PER_PAGE; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= mappingEntries.size()) break;
            Map.Entry<String, WorldMapping> entry = mappingEntries.get(entryIndex);
            String key = entry.getKey();
            WorldMapping wm = entry.getValue();
            String label = (wm.displayName.isBlank() ? shortenContext(key) : wm.displayName)
                    + (wm.enabled ? " §a✓" : " §c✗");
            final String finalKey = key;
            addRenderableWidget(new Button(centerX - 105, startY + i * rowHeight, 200, 20,
                    Component.literal(label), b -> this.minecraft.setScreen(new MappingEditScreen(this, finalKey))));
        }

        int bottomY = this.height - 30;
        addRenderableWidget(new Button(centerX - 120, bottomY, 110, 20,
                Component.translatable("gui.discraft.config.add_mapping"),
                btn -> this.minecraft.setScreen(new AddMappingScreen(this))));

        addRenderableWidget(new Button(centerX + 10, bottomY, 110, 20,
                Component.translatable("gui.discraft.config.done"), btn -> onClose()));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        renderBackground(poseStack);

        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 14, 0xFFFFFF);

        Component statusText = DisCraft.BRIDGE.isVoiceConnected()
                ? Component.translatable("gui.discraft.config.voice_connected")
                : Component.translatable("gui.discraft.config.voice_disconnected");
        drawCenteredString(poseStack, this.font, statusText, this.width / 2, 88, 0xFFFFFF);

        drawCenteredString(poseStack, this.font, Component.translatable("gui.discraft.config.mappings_title"),
                this.width / 2, 108, 0xFFFFFF);

        super.render(poseStack, mouseX, mouseY, delta);
    }

    private void refreshMappingEntries() {
        mappingEntries = new ArrayList<>(DisCraft.CONFIG.mappings.entrySet());
    }

    private static String shortenContext(String ctx) {
        if (ctx.startsWith("world:")) return "💾 " + ctx.substring(6);
        if (ctx.startsWith("server:")) return "🖥 " + ctx.substring(7);
        return ctx;
    }

    @Override
    public void onClose() {
        DisCraft.CONFIG.save();
        this.minecraft.setScreen(parent);
    }
}
