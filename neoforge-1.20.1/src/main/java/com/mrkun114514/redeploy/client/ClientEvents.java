package com.mrkun114514.redeploy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Replaces the vanilla {@link DeathScreen} with our COD-style screen the moment
 * it is about to open.
 *
 * <p>NeoForge 1.20.1 uses the classic Forge event bus
 * ({@code net.minecraftforge.common.MinecraftForge#EVENT_BUS}) and the
 * {@code net.minecraftforge.client.event.ScreenEvent.Opening} intercept.</p>
 */
public class ClientEvents {

    /** Register this handler on the Forge game event bus. Call from client init. */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof DeathScreen) {
            Minecraft mc = Minecraft.getInstance();
            boolean hardcore = mc.level != null && mc.level.getLevelData().isHardcore();
            event.setNewScreen(new RedeployDeathScreen(hardcore));
        }
    }
}
