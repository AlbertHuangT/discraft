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
        super(Text.translatable("gui.discraft.mapping.title"));
        this.parent = parent;
        this.contextKey = contextKey;
        WorldMapping existing = DisCraft.CONFIG.getMapping(contextKey);
        this.mapping = (existing != null) ? existing : new WorldMapping();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 46;
        int fieldWidth = 280;
        int fieldHeight = 20;
        int gap = 32;

        // 显示名
        displayNameField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, startY, fieldWidth, fieldHeight,
                Text.translatable("gui.discraft.mapping.label.name"));
        displayNameField.setMaxLength(64);
        displayNameField.setText(mapping.displayName);
        String hintName = Text.translatable("gui.discraft.mapping.hint.name").getString();
        displayNameField.setSuggestion(mapping.displayName.isEmpty() ? hintName : "");
        displayNameField.setChangedListener(t -> displayNameField.setSuggestion(t.isEmpty() ? hintName : ""));
        addDrawableChild(displayNameField);

        // Webhook URL
        int y2 = startY + gap;
        webhookUrlField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, y2, fieldWidth, fieldHeight,
                Text.translatable("gui.discraft.mapping.label.webhook"));
        webhookUrlField.setMaxLength(256);
        webhookUrlField.setText(mapping.webhookUrl);
        String hintWebhook = Text.translatable("gui.discraft.mapping.hint.webhook").getString();
        webhookUrlField.setSuggestion(mapping.webhookUrl.isEmpty() ? hintWebhook : "");
        webhookUrlField.setChangedListener(t -> webhookUrlField.setSuggestion(t.isEmpty() ? hintWebhook : ""));
        addDrawableChild(webhookUrlField);

        // 语音频道 ID
        int y3 = y2 + gap;
        voiceChannelIdField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, y3, fieldWidth, fieldHeight,
                Text.translatable("gui.discraft.mapping.label.voice"));
        voiceChannelIdField.setMaxLength(32);
        voiceChannelIdField.setText(mapping.voiceChannelId);
        String hintVoice = Text.translatable("gui.discraft.mapping.hint.voice").getString();
        voiceChannelIdField.setSuggestion(mapping.voiceChannelId.isEmpty() ? hintVoice : "");
        voiceChannelIdField.setChangedListener(t -> voiceChannelIdField.setSuggestion(t.isEmpty() ? hintVoice : ""));
        addDrawableChild(voiceChannelIdField);

        // 复选框
        int checkY = y3 + gap + 4;
        int col1 = centerX - 140;
        int col2 = centerX + 10;

        enabledCheck = addDrawableChild(new CheckboxWidget(col1, checkY, 140, 20,
                Text.translatable("gui.discraft.mapping.enabled"), mapping.enabled));
        sendCheck = addDrawableChild(new CheckboxWidget(col2, checkY, 160, 20,
                Text.translatable("gui.discraft.mapping.forward_chat"), mapping.sendToDiscord));

        int checkY2 = checkY + 24;
        joinLeaveCheck = addDrawableChild(new CheckboxWidget(col1, checkY2, 140, 20,
                Text.translatable("gui.discraft.mapping.show_join_leave"), mapping.showJoinLeave));
        deathsCheck = addDrawableChild(new CheckboxWidget(col2, checkY2, 160, 20,
                Text.translatable("gui.discraft.mapping.show_death"), mapping.showDeaths));

        int checkY3 = checkY2 + 24;
        advancementsCheck = addDrawableChild(new CheckboxWidget(col1, checkY3, 180, 20,
                Text.translatable("gui.discraft.mapping.show_advancements"), mapping.showAdvancements));

        // 底部按钮
        int buttonY = this.height - 40;
        addDrawableChild(new ButtonWidget(centerX - 110, buttonY, 100, 20,
                Text.translatable("gui.discraft.mapping.save"), btn -> saveAndClose()));

        addDrawableChild(new ButtonWidget(centerX - 5, buttonY, 100, 20,
                Text.translatable("gui.discraft.mapping.delete"), btn -> deleteAndClose()));

        addDrawableChild(new ButtonWidget(centerX + 110, buttonY, 60, 20,
                Text.translatable("gui.discraft.mapping.cancel"), btn -> client.setScreen(parent)));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        drawCenteredText(matrices, textRenderer, this.title, this.width / 2, 14, 0xFFFFFF);
        drawCenteredText(matrices, textRenderer,
                Text.translatable("gui.discraft.mapping.context_label").append(contextKey),
                this.width / 2, 26, 0xAAAAAA);

        int centerX = this.width / 2;
        int fieldWidth = 280;
        int startY = 46;
        int gap = 32;
        int labelX = centerX - fieldWidth / 2;

        drawTextWithShadow(matrices, textRenderer, Text.translatable("gui.discraft.mapping.label.name"), labelX, startY - 10, 0xCCCCCC);
        drawTextWithShadow(matrices, textRenderer, Text.translatable("gui.discraft.mapping.label.webhook"), labelX, startY + gap - 10, 0xCCCCCC);
        drawTextWithShadow(matrices, textRenderer, Text.translatable("gui.discraft.mapping.label.voice"), labelX, startY + gap * 2 - 10, 0xCCCCCC);

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
