package com.discraft.gui;

import com.discraft.DisCraft;
import com.discraft.config.WorldMapping;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DisCraft 主设置界面。
 * 显示：Bot Token 设置、当前上下文、所有已有映射列表。
 */
public class ConfigScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget botTokenField;

    // 映射列表分页
    private int scrollOffset = 0;
    private static final int ENTRIES_PER_PAGE = 4;
    private List<Map.Entry<String, WorldMapping>> mappingEntries = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        super(Text.literal("DisCraft 设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshMappingEntries();

        int centerX = this.width / 2;
        int fieldWidth = 300;

        // Bot Token 输入框
        botTokenField = addDrawableChild(new TextFieldWidget(
                textRenderer, centerX - fieldWidth / 2, 48, fieldWidth, 20,
                Text.literal("Bot Token")));
        botTokenField.setMaxLength(128);
        botTokenField.setText(DisCraft.CONFIG.botToken);
        botTokenField.setSuggestion("Discord Bot Token（接收消息必填）");

        // 保存 Token 按钮
        addDrawableChild(new ButtonWidget(centerX + fieldWidth / 2 + 4, 48, 70, 20,
                Text.literal("保存 Token"), btn -> {
            DisCraft.CONFIG.botToken = botTokenField.getText().trim();
            DisCraft.CONFIG.save();
            // 如果当前有活跃上下文，重新连接 Gateway
            String ctx = DisCraft.BRIDGE.getCurrentContext();
            if (ctx != null) {
                DisCraft.BRIDGE.onContextSwitch(ctx, client);
            }
        }));

        // 当前上下文的快捷编辑按钮
        String ctx = DisCraft.BRIDGE.getCurrentContext();
        String ctxLabel = (ctx != null)
                ? "编辑当前：" + shortenContext(ctx)
                : "未在游戏中";
        addDrawableChild(new ButtonWidget(centerX - 100, 78, 200, 20,
                Text.literal(ctxLabel), btn -> {
            if (ctx != null) client.setScreen(new MappingEditScreen(this, ctx));
        }));

        // 映射列表翻页按钮
        addDrawableChild(new ButtonWidget(centerX - 110, 106, 20, 20,
                Text.literal("◀"), btn -> {
            if (scrollOffset > 0) {
                String token = botTokenField.getText();
                scrollOffset--;
                this.init(client, this.width, this.height);
                botTokenField.setText(token);
            }
        }));

        addDrawableChild(new ButtonWidget(centerX + 90, 106, 20, 20,
                Text.literal("▶"), btn -> {
            int maxOffset = Math.max(0, mappingEntries.size() - ENTRIES_PER_PAGE);
            if (scrollOffset < maxOffset) {
                String token = botTokenField.getText();
                scrollOffset++;
                this.init(client, this.width, this.height);
                botTokenField.setText(token);
            }
        }));

        // 映射条目按钮
        int startY = 140;
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
                Text.literal("添加新映射"), btn -> client.setScreen(new AddMappingScreen(this))));

        addDrawableChild(new ButtonWidget(centerX + 10, bottomY, 110, 20,
                Text.literal("完成"), btn -> close()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        // 标题
        drawCenteredText(matrices, textRenderer, this.title, this.width / 2, 14, 0xFFFFFF);

        // Bot Token 标签
        drawTextWithShadow(matrices, textRenderer, Text.literal("Bot Token（接收 Discord 消息必填）："),
                this.width / 2 - 150, 38, 0xCCCCCC);

        // 连接状态
        String statusText = DisCraft.BRIDGE.isGatewayConnected()
                ? "§a● 已连接 Discord" : "§7○ 未连接";
        drawTextWithShadow(matrices, textRenderer, Text.literal(statusText),
                this.width / 2 - 150, 102, 0xFFFFFF);

        // 映射列表标题
        drawTextWithShadow(matrices, textRenderer, Text.literal("§e所有频道映射："),
                this.width / 2 - 150, 128, 0xFFFFFF);

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
