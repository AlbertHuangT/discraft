package com.discraft.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AddMappingScreen extends Screen {

    private final Screen parent;
    private EditBox contextKeyField;

    public AddMappingScreen(Screen parent) {
        super(Component.translatable("gui.discraft.add.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        contextKeyField = addRenderableWidget(new EditBox(
                this.font, centerX - 140, this.height / 2 - 10, 280, 20,
                Component.translatable("gui.discraft.add.context_label")));
        contextKeyField.setMaxLength(128);
        contextKeyField.setSuggestion(Component.translatable("gui.discraft.add.hint").getString());

        int btnY = this.height / 2 + 20;
        addRenderableWidget(new Button(centerX - 55, btnY, 50, 20,
                Component.translatable("gui.discraft.add.next"), btn -> {
            String key = contextKeyField.getValue().trim();
            if (!key.isBlank()) {
                this.minecraft.setScreen(new MappingEditScreen(parent, key));
            }
        }));

        addRenderableWidget(new Button(centerX + 5, btnY, 50, 20,
                Component.translatable("gui.discraft.add.cancel"), btn -> this.minecraft.setScreen(parent)));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        drawCenteredString(poseStack, this.font,
                Component.translatable("gui.discraft.add.tip"),
                this.width / 2, this.height / 2 - 30, 0xAAAAAA);
        this.font.drawShadow(poseStack,
                Component.translatable("gui.discraft.add.context_label"),
                (float)(this.width / 2 - 140), this.height / 2 - 22, 0xCCCCCC);
        super.render(poseStack, mouseX, mouseY, delta);
    }
}
