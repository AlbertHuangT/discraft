package com.discraft.gui;

import com.discraft.DisCraft;
import com.discraft.config.WorldMapping;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DisCraft 主设置界面。
 * 显示：Discord 授权状态、当前上下文、所有已有映射列表。
 */
public class ConfigScreen extends Screen {

    private final Screen parent;

    // 映射列表分页
    private int scrollOffset = 0;
    private static final int ENTRIES_PER_PAGE = 3;
    private List<Map.Entry<String, WorldMapping>> mappingEntries = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        super(Text.translatable("gui.discraft.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshMappingEntries();

        int centerX = this.width / 2;

        // 授权 Discord 按钮（Y=40）
        addDrawableChild(new ButtonWidget(centerX - 100, 40, 200, 20,
                Text.translatable("gui.discraft.config.authorize"), btn -> {
            btn.active = false;
            DisCraft.BRIDGE.startIpcAuth(client);
            Thread t = new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                client.execute(() -> btn.active = true);
            });
            t.setDaemon(true);
            t.start();
        }));

        // 当前上下文的快捷编辑按钮（Y=64）
        String ctx = DisCraft.BRIDGE.getCurrentContext();
        Text ctxLabel = (ctx != null)
                ? Text.translatable("gui.discraft.config.edit_current").append(shortenContext(ctx))
                : Text.translatable("gui.discraft.config.not_in_game");
        addDrawableChild(new ButtonWidget(centerX - 100, 64, 200, 20,
                ctxLabel, btn -> {
            if (ctx != null) client.setScreen(new MappingEditScreen(this, ctx));
        }));

        // 映射列表翻页按钮（Y=92）
        addDrawableChild(new ButtonWidget(centerX - 110, 92, 20, 20,
                Text.literal("◀"), btn -> {
            if (scrollOffset > 0) {
                scrollOffset--;
                this.init(client, this.width, this.height);
            }
        }));

        addDrawableChild(new ButtonWidget(centerX + 90, 92, 20, 20,
                Text.literal("▶"), btn -> {
            int maxOffset = Math.max(0, mappingEntries.size() - ENTRIES_PER_PAGE);
            if (scrollOffset < maxOffset) {
                scrollOffset++;
                this.init(client, this.width, this.height);
            }
        }));

        // 映射条目按钮（Y=122+）
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
            addDrawableChild(new ButtonWidget(centerX - 105, startY + i * rowHeight, 200, 20,
                    Text.literal(label), b -> client.setScreen(new MappingEditScreen(this, finalKey))));
        }

        // 底部按钮
        int bottomY = this.height - 30;
        addDrawableChild(new ButtonWidget(centerX - 120, bottomY, 110, 20,
                Text.translatable("gui.discraft.config.add_mapping"), btn -> client.setScreen(new AddMappingScreen(this))));

        addDrawableChild(new ButtonWidget(centerX + 10, bottomY, 110, 20,
                Text.translatable("gui.discraft.config.done"), btn -> close()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        // 标题
        drawCenteredText(matrices, textRenderer, this.title, this.width / 2, 14, 0xFFFFFF);

        // 语音连接状态（Y=88）
        Text statusText = DisCraft.BRIDGE.isVoiceConnected()
                ? Text.translatable("gui.discraft.config.voice_connected")
                : Text.translatable("gui.discraft.config.voice_disconnected");
        drawTextWithShadow(matrices, textRenderer, statusText,
                this.width / 2 - 150, 88, 0xFFFFFF);

        // 映射列表标题（Y=108）
        drawTextWithShadow(matrices, textRenderer, Text.translatable("gui.discraft.config.mappings_title"),
                this.width / 2 - 150, 108, 0xFFFFFF);

        super.render(matrices, mouseX, mouseY, delta);
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
    public void close() {
        DisCraft.CONFIG.save();
        client.setScreen(parent);
    }
}
