package com.discraft.gui;

import com.discraft.DisCraft;
import com.discraft.config.WorldMapping;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * 编辑单个存档/服务器的 Discord 频道映射配置。
 */
public class MappingEditScreen extends Screen {

    private final Screen parent;
    private final String contextKey;
    private WorldMapping mapping;

    private TextFieldWidget displayNameField;
    private TextFieldWidget webhookUrlField;
    private TextFieldWidget voiceChannelIdField;
    private CheckboxWidget enabledCheck;
    private CheckboxWidget sendCheck;
    private CheckboxWidget joinLeaveCheck;
    private CheckboxWidget deathsCheck;
    private CheckboxWidget advancementsCheck;

    public MappingEditScreen(Screen parent, String contextKey) {
        super(Text.literal("编辑 Discord 频道映射"));
        this.parent = parent;
        this.contextKey = contextKey;
        WorldMapping existing = DisCraft.CONFIG.getMapping(contextKey);
        this.mapping = (existing != null) ? existing : new WorldMapping();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 40;
        int fieldWidth = 280;
        int fieldHeight = 20;
        int gap = 28;

        // 显示名
        displayNameField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, startY, fieldWidth, fieldHeight,
                Text.literal("显示名"));
        displayNameField.setMaxLength(64);
        displayNameField.setText(mapping.displayName);
        displayNameField.setSuggestion("例：我的生存服（可选）");
        addDrawableChild(displayNameField);

        // Webhook URL
        int y2 = startY + gap;
        webhookUrlField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, y2, fieldWidth, fieldHeight,
                Text.literal("Webhook URL"));
        webhookUrlField.setMaxLength(256);
        webhookUrlField.setText(mapping.webhookUrl);
        webhookUrlField.setSuggestion("https://discord.com/api/webhooks/...");
        addDrawableChild(webhookUrlField);

        // 语音频道 ID
        int y3 = y2 + gap;
        voiceChannelIdField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, y3, fieldWidth, fieldHeight,
                Text.literal("语音频道 ID"));
        voiceChannelIdField.setMaxLength(32);
        voiceChannelIdField.setText(mapping.voiceChannelId);
        voiceChannelIdField.setSuggestion("Discord 语音频道 ID（进入存档时自动加入）");
        addDrawableChild(voiceChannelIdField);

        // 复选框
        int checkY = y3 + gap + 4;
        int col1 = centerX - 140;
        int col2 = centerX + 10;

        enabledCheck = addDrawableChild(new CheckboxWidget(col1, checkY, 140, 20,
                Text.literal("启用此映射"), mapping.enabled));
        sendCheck = addDrawableChild(new CheckboxWidget(col2, checkY, 160, 20,
                Text.literal("发送游戏消息到 Discord"), mapping.sendToDiscord));

        int checkY2 = checkY + 24;
        joinLeaveCheck = addDrawableChild(new CheckboxWidget(col1, checkY2, 140, 20,
                Text.literal("显示加入/离开"), mapping.showJoinLeave));
        deathsCheck = addDrawableChild(new CheckboxWidget(col2, checkY2, 160, 20,
                Text.literal("显示死亡事件"), mapping.showDeaths));

        int checkY3 = checkY2 + 24;
        advancementsCheck = addDrawableChild(new CheckboxWidget(col1, checkY3, 180, 20,
                Text.literal("显示成就通知"), mapping.showAdvancements));

        // 底部按钮
        int buttonY = this.height - 40;
        addDrawableChild(new ButtonWidget(centerX - 110, buttonY, 100, 20,
                Text.literal("保存"), btn -> saveAndClose()));

        addDrawableChild(new ButtonWidget(centerX - 5, buttonY, 100, 20,
                Text.literal("删除映射"), btn -> deleteAndClose()));

        addDrawableChild(new ButtonWidget(centerX + 110, buttonY, 60, 20,
                Text.literal("取消"), btn -> client.setScreen(parent)));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        drawCenteredText(matrices, textRenderer, this.title, this.width / 2, 14, 0xFFFFFF);
        drawCenteredText(matrices, textRenderer,
                Text.literal("§7上下文: " + contextKey), this.width / 2, 26, 0xAAAAAA);

        int centerX = this.width / 2;
        int fieldWidth = 280;
        int startY = 40;
        int gap = 28;
        int labelX = centerX - fieldWidth / 2;

        drawTextWithShadow(matrices, textRenderer, Text.literal("显示名"), labelX, startY - 10, 0xCCCCCC);
        drawTextWithShadow(matrices, textRenderer, Text.literal("Webhook URL（发送消息用）"), labelX, startY + gap - 10, 0xCCCCCC);
        drawTextWithShadow(matrices, textRenderer, Text.literal("语音频道 ID（进入存档时自动加入）"), labelX, startY + gap * 2 - 10, 0xCCCCCC);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void saveAndClose() {
        mapping.displayName = displayNameField.getText().trim();
        mapping.webhookUrl = webhookUrlField.getText().trim();
        mapping.voiceChannelId = voiceChannelIdField.getText().trim();
        mapping.enabled = enabledCheck.isChecked();
        mapping.sendToDiscord = sendCheck.isChecked();
        mapping.showJoinLeave = joinLeaveCheck.isChecked();
        mapping.showDeaths = deathsCheck.isChecked();
        mapping.showAdvancements = advancementsCheck.isChecked();

        DisCraft.CONFIG.setMapping(contextKey, mapping);
        DisCraft.CONFIG.save();
        client.setScreen(parent);
    }

    private void deleteAndClose() {
        DisCraft.CONFIG.removeMapping(contextKey);
        DisCraft.CONFIG.save();
        client.setScreen(parent);
    }
}
