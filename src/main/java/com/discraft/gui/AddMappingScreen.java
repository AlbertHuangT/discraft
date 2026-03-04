package com.discraft.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * 手动添加新映射——输入上下文 key（适用于还没进入对应存档/服务器的情况）。
 * 通常情况下进入游戏后直接按 G 就会自动识别上下文，这个界面是高级用途。
 */
public class AddMappingScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget contextKeyField;

    public AddMappingScreen(Screen parent) {
        super(Text.literal("添加新映射"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        contextKeyField = addDrawableChild(new TextFieldWidget(
                textRenderer, centerX - 140, this.height / 2 - 10, 280, 20,
                Text.literal("上下文 key")));
        contextKeyField.setMaxLength(128);
        contextKeyField.setSuggestion("例：server:hypixel.net 或 world:我的存档");

        int btnY = this.height / 2 + 20;
        addDrawableChild(new ButtonWidget(centerX - 55, btnY, 50, 20,
                Text.literal("下一步"), btn -> {
            String key = contextKeyField.getText().trim();
            if (!key.isBlank()) {
                client.setScreen(new MappingEditScreen(parent, key));
            }
        }));

        addDrawableChild(new ButtonWidget(centerX + 5, btnY, 50, 20,
                Text.literal("取消"), btn -> client.setScreen(parent)));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        drawCenteredText(matrices, textRenderer,
                Text.literal("§7进入存档/服务器后按 G 可自动识别上下文"),
                this.width / 2, this.height / 2 - 30, 0xAAAAAA);
        drawTextWithShadow(matrices, textRenderer,
                Text.literal("上下文 Key："),
                this.width / 2 - 140, this.height / 2 - 22, 0xCCCCCC);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
