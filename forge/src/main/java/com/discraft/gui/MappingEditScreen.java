package com.discraft.gui;

import com.discraft.DisCraft;
import com.discraft.config.WorldMapping;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MappingEditScreen extends Screen {

    private final Screen parent;
    private final String contextKey;
    private WorldMapping mapping;

    private EditBox displayNameField;
    private EditBox webhookUrlField;
    private EditBox voiceChannelIdField;
    private Checkbox enabledCheck;
    private Checkbox sendCheck;
    private Checkbox joinLeaveCheck;
    private Checkbox deathsCheck;
    private Checkbox advancementsCheck;

    public MappingEditScreen(Screen parent, String contextKey) {
        super(Component.translatable("gui.discraft.mapping.title"));
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

        displayNameField = new EditBox(this.font, centerX - fieldWidth / 2, startY, fieldWidth, fieldHeight,
                Component.translatable("gui.discraft.mapping.label.name"));
        displayNameField.setMaxLength(64);
        displayNameField.setValue(mapping.displayName);
        String hintName = Component.translatable("gui.discraft.mapping.hint.name").getString();
        displayNameField.setSuggestion(mapping.displayName.isEmpty() ? hintName : "");
        displayNameField.setResponder(t -> displayNameField.setSuggestion(t.isEmpty() ? hintName : ""));
        addRenderableWidget(displayNameField);

        int y2 = startY + gap;
        webhookUrlField = new EditBox(this.font, centerX - fieldWidth / 2, y2, fieldWidth, fieldHeight,
                Component.translatable("gui.discraft.mapping.label.webhook"));
        webhookUrlField.setMaxLength(256);
        webhookUrlField.setValue(mapping.webhookUrl);
        String hintWebhook = Component.translatable("gui.discraft.mapping.hint.webhook").getString();
        webhookUrlField.setSuggestion(mapping.webhookUrl.isEmpty() ? hintWebhook : "");
        webhookUrlField.setResponder(t -> webhookUrlField.setSuggestion(t.isEmpty() ? hintWebhook : ""));
        addRenderableWidget(webhookUrlField);

        int y3 = y2 + gap;
        voiceChannelIdField = new EditBox(this.font, centerX - fieldWidth / 2, y3, fieldWidth, fieldHeight,
                Component.translatable("gui.discraft.mapping.label.voice"));
        voiceChannelIdField.setMaxLength(32);
        voiceChannelIdField.setValue(mapping.voiceChannelId);
        String hintVoice = Component.translatable("gui.discraft.mapping.hint.voice").getString();
        voiceChannelIdField.setSuggestion(mapping.voiceChannelId.isEmpty() ? hintVoice : "");
        voiceChannelIdField.setResponder(t -> voiceChannelIdField.setSuggestion(t.isEmpty() ? hintVoice : ""));
        addRenderableWidget(voiceChannelIdField);

        int checkY = y3 + gap + 4;
        int col1 = centerX - 140;
        int col2 = centerX + 10;

        enabledCheck = addRenderableWidget(new Checkbox(col1, checkY, 140, 20,
                Component.translatable("gui.discraft.mapping.enabled"), mapping.enabled));
        sendCheck = addRenderableWidget(new Checkbox(col2, checkY, 160, 20,
                Component.translatable("gui.discraft.mapping.forward_chat"), mapping.sendToDiscord));

        int checkY2 = checkY + 24;
        joinLeaveCheck = addRenderableWidget(new Checkbox(col1, checkY2, 140, 20,
                Component.translatable("gui.discraft.mapping.show_join_leave"), mapping.showJoinLeave));
        deathsCheck = addRenderableWidget(new Checkbox(col2, checkY2, 160, 20,
                Component.translatable("gui.discraft.mapping.show_death"), mapping.showDeaths));

        int checkY3 = checkY2 + 24;
        advancementsCheck = addRenderableWidget(new Checkbox(col1, checkY3, 180, 20,
                Component.translatable("gui.discraft.mapping.show_advancements"), mapping.showAdvancements));

        int buttonY = this.height - 40;
        addRenderableWidget(new Button(centerX - 110, buttonY, 100, 20,
                Component.translatable("gui.discraft.mapping.save"), btn -> saveAndClose()));

        addRenderableWidget(new Button(centerX - 5, buttonY, 100, 20,
                Component.translatable("gui.discraft.mapping.delete"), btn -> deleteAndClose()));

        addRenderableWidget(new Button(centerX + 110, buttonY, 60, 20,
                Component.translatable("gui.discraft.mapping.cancel"), btn -> this.minecraft.setScreen(parent)));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);

        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        drawCenteredString(poseStack, this.font,
                Component.translatable("gui.discraft.mapping.context_label").append(contextKey),
                this.width / 2, 26, 0xAAAAAA);

        int centerX = this.width / 2;
        int fieldWidth = 280;
        int startY = 46;
        int gap = 32;
        int labelX = centerX - fieldWidth / 2;

        this.font.drawShadow(poseStack, Component.translatable("gui.discraft.mapping.label.name"),
                (float)labelX, startY - 10, 0xCCCCCC);
        this.font.drawShadow(poseStack, Component.translatable("gui.discraft.mapping.label.webhook"),
                (float)labelX, startY + gap - 10, 0xCCCCCC);
        this.font.drawShadow(poseStack, Component.translatable("gui.discraft.mapping.label.voice"),
                (float)labelX, startY + gap * 2 - 10, 0xCCCCCC);

        super.render(poseStack, mouseX, mouseY, delta);
    }

    private void saveAndClose() {
        mapping.displayName = displayNameField.getValue().trim();
        mapping.webhookUrl = webhookUrlField.getValue().trim();
        mapping.voiceChannelId = voiceChannelIdField.getValue().trim();
        mapping.enabled = enabledCheck.selected();
        mapping.sendToDiscord = sendCheck.selected();
        mapping.showJoinLeave = joinLeaveCheck.selected();
        mapping.showDeaths = deathsCheck.selected();
        mapping.showAdvancements = advancementsCheck.selected();

        DisCraft.CONFIG.setMapping(contextKey, mapping);
        DisCraft.CONFIG.save();
        this.minecraft.setScreen(parent);
    }

    private void deleteAndClose() {
        DisCraft.CONFIG.removeMapping(contextKey);
        DisCraft.CONFIG.save();
        this.minecraft.setScreen(parent);
    }
}
