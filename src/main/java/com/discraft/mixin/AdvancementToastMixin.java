package com.discraft.mixin;

import com.discraft.DisCraft;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.client.toast.AdvancementToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截成就 Toast 的创建，将成就信息发送到 Discord。
 */
@Mixin(AdvancementToast.class)
public class AdvancementToastMixin {

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/advancement/Advancement;)V")
    private void onInit(Advancement advancement, CallbackInfo ci) {
        if (DisCraft.BRIDGE == null) return;
        AdvancementDisplay display = advancement.getDisplay();
        if (display == null) return;
        DisCraft.BRIDGE.onAdvancement(display.getTitle(), display.getDescription());
    }
}
