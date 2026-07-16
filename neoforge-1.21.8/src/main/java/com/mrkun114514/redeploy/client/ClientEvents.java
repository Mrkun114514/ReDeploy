package com.mrkun114514.redeploy.client;

import com.mrkun114514.redeploy.Redeploy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Replaces the vanilla {@link DeathScreen} with our COD-style screen
 * the moment it is about to open.
 *
 * <p>NeoForge 21.8 removed {@code @EventBusSubscriber} / {@code Bus}, so we register
 * this handler manually on the game event bus (see {@link Redeploy}).</p>
 */
public class ClientEvents {

    /** Register this handler on the NeoForge game event bus. Call from client init. */
    public static void register() {
        NeoForge.EVENT_BUS.register(ClientEvents.class);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof DeathScreen) {
            // Hardcore deaths use a different vanilla screen intent ("you are done");
            // derive it from the world's hardcore flag (stable LevelData API).
            Minecraft mc = Minecraft.getInstance();
            boolean hardcore = mc.level != null && mc.level.getLevelData().isHardcore();
            event.setNewScreen(new RedeployDeathScreen(hardcore));
        }
    }
}
