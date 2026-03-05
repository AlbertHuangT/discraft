package com.discraft.mixin;

import com.discraft.DisCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Inject(method = "handleChatInput", at = @At("HEAD"))
    private void discraft$onHandleChatInput(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (!chatText.startsWith("/")) {
            DisCraft.BRIDGE.onPlayerChat(chatText, Minecraft.getInstance());
        }
    }
}
