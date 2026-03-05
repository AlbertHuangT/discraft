package com.discraft.mixin;

import com.discraft.DisCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    // handleChatInput 返回 boolean，必须用 CallbackInfoReturnable
    @Inject(method = "handleChatInput", at = @At("HEAD"))
    private void discraft$onHandleChatInput(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        if (!chatText.startsWith("/")) {
            DisCraft.BRIDGE.onPlayerChat(chatText, Minecraft.getInstance());
        }
    }
}
