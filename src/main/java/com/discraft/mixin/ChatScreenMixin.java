package com.discraft.mixin;

import com.discraft.DisCraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Inject(method = "sendMessage", at = @At("HEAD"))
    private void discraft$onSendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        // 只处理普通聊天（非命令）
        if (!chatText.startsWith("/")) {
            DisCraft.BRIDGE.onPlayerChat(chatText, MinecraftClient.getInstance());
        }
    }
}
