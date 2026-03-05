package com.discraft.mixin;

import com.discraft.DisCraft;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementToast.class)
public class AdvancementToastMixin {

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/advancements/Advancement;)V")
    private void onInit(Advancement advancement, CallbackInfo ci) {
        if (DisCraft.BRIDGE == null) return;
        DisplayInfo display = advancement.getDisplay();
        if (display == null) return;
        DisCraft.BRIDGE.onAdvancement(display.getTitle(), display.getDescription());
    }
}
