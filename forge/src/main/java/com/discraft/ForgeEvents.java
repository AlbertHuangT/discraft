package com.discraft;

import com.discraft.gui.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeEvents {

    @SubscribeEvent
    public void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();
        String context = DisCraft.resolveContext(mc);
        DisCraft.LOGGER.info("[DisCraft] 上下文切换: {}", context);
        DisCraft.BRIDGE.onContextSwitch(context, mc);
    }

    @SubscribeEvent
    public void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft mc = Minecraft.getInstance();
        DisCraft.BRIDGE.onDisconnect(mc);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (DisCraft.CONFIG_KEY == null) return;
        Minecraft mc = Minecraft.getInstance();
        while (DisCraft.CONFIG_KEY.consumeClick()) {
            mc.setScreen(new ConfigScreen(mc.screen));
        }
    }
}
