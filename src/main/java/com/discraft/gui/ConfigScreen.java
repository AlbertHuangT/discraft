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
 * 显示：Discord 授权状态、当前上下文、所有已有映射列表。
 */
public class ConfigScreen extends Screen {

    private final Screen parent;

    // 凭据输入框
    private TextFieldWidget clientIdField;
    private TextFieldWidget clientSecretField;

    // 映射列表分页
    private int scrollOffset = 0;
    private static final int ENTRIES_PER_PAGE = 3;
    private List<Map.Entry<String, WorldMapping>> mappingEntries = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        super(Text.literal("DisCraft 设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshMappingEntries();

        int centerX = this.width / 2;
        int fieldWidth = 220;
        int gap = 4;
        // 两个输入框并排，总宽 = fieldWidth*2 + gap
        int totalFieldWidth = fieldWidth * 2 + gap;
        int leftFieldX = centerX - totalFieldWidth / 2;
        int rightFieldX = leftFieldX + fieldWidth + gap;

        // Client ID 输入框
        clientIdField = new TextFieldWidget(textRenderer, leftFieldX, 30, fieldWidth, 18,
                Text.literal("Client ID"));
        clientIdField.setMaxLength(64);
        clientIdField.setText(DisCraft.CONFIG.discordClientId);
        addDrawableChild(clientIdField);

        // Client Secret 输入框
        clientSecretField = new TextFieldWidget(textRenderer, rightFieldX, 30, fieldWidth, 18,
                Text.literal("Client Secret"));
        clientSecretField.setMaxLength(64);
        clientSecretField.setText(DisCraft.CONFIG.discordClientSecret);
        addDrawableChild(clientSecretField);

        // 保存应用设置 + 授权 Discord 按钮（Y=54）
        addDrawableChild(new ButtonWidget(centerX - 210, 54, 100, 20,
                Text.literal("保存应用设置"), btn -> {
            DisCraft.CONFIG.discordClientId = clientIdField.getText().strip();
            DisCraft.CONFIG.discordClientSecret = clientSecretField.getText().strip();
            DisCraft.CONFIG.save();
        }));

        addDrawableChild(new ButtonWidget(centerX - 100, 54, 200, 20,
                Text.literal("授权 Discord（语音功能必须）"), btn -> {
            btn.active = false;
            DisCraft.BRIDGE.startIpcAuth(client);
            Thread t = new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                client.execute(() -> btn.active = true);
            });
            t.setDaemon(true);
            t.start();
        }));

        // 当前上下文的快捷编辑按钮（Y=78）
        String ctx = DisCraft.BRIDGE.getCurrentContext();
        String ctxLabel = (ctx != null)
                ? "编辑当前：" + shortenContext(ctx)
                : "未在游戏中";
        addDrawableChild(new ButtonWidget(centerX - 100, 78, 200, 20,
                Text.literal(ctxLabel), btn -> {
            if (ctx != null) client.setScreen(new MappingEditScreen(this, ctx));
        }));

        // 映射列表翻页按钮（Y=104）
        addDrawableChild(new ButtonWidget(centerX - 110, 104, 20, 20,
                Text.literal("◀"), btn -> {
            if (scrollOffset > 0) {
                scrollOffset--;
                this.init(client, this.width, this.height);
            }
        }));

        addDrawableChild(new ButtonWidget(centerX + 90, 104, 20, 20,
                Text.literal("▶"), btn -> {
            int maxOffset = Math.max(0, mappingEntries.size() - ENTRIES_PER_PAGE);
            if (scrollOffset < maxOffset) {
                scrollOffset++;
                this.init(client, this.width, this.height);
            }
        }));

        // 映射条目按钮（Y=134+）
        int startY = 134;
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

        // 凭据输入框标签
        drawTextWithShadow(matrices, textRenderer, Text.literal("§7Client ID"),
                this.width / 2 - (220 * 2 + 4) / 2, 20, 0xFFFFFF);
        drawTextWithShadow(matrices, textRenderer, Text.literal("§7Client Secret"),
                this.width / 2 - (220 * 2 + 4) / 2 + 220 + 4, 20, 0xFFFFFF);

        // 语音连接状态
        String statusText = DisCraft.BRIDGE.isVoiceConnected()
                ? "§a● 语音已连接" : "§7○ Discord 未运行或未授权";
        drawTextWithShadow(matrices, textRenderer, Text.literal(statusText),
                this.width / 2 - 150, 100, 0xFFFFFF);

        // 映射列表标题
        drawTextWithShadow(matrices, textRenderer, Text.literal("§e所有频道映射："),
                this.width / 2 - 150, 122, 0xFFFFFF);

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
