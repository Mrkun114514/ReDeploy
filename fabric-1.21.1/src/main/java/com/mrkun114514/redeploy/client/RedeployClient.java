package com.mrkun114514.redeploy.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

/**
 * Fabric client entry point for ReDeploy (Minecraft 1.21.1).
 *
 * <p>Fabric has no equivalent of NeoForge's {@code ScreenEvent.Opening}, so instead
 * of a Mixin we watch the client tick: whenever the vanilla {@link DeathScreen}
 * becomes the active screen we swap it for our {@link RedeployDeathScreen}. This is
 * loader-version agnostic and needs only fabric-api, avoiding fragile Mixin refmaps
 * across Minecraft versions.</p>
 *
 * @author Mrkun114514
 */
public class RedeployClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
    }

    private void onEndTick(Minecraft mc) {
        // Only react to the vanilla death screen; our replacement extends Screen
        // (not DeathScreen), so this never re-triggers on our own screen.
        if (mc.screen instanceof DeathScreen) {
            boolean hardcore = mc.level != null && mc.level.getLevelData().isHardcore();
            mc.setScreen(new RedeployDeathScreen(hardcore));
        }
    }
}
