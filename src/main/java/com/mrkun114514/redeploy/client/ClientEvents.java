package com.mrkun114514.redeploy.client;

import com.mrkun114514.redeploy.Redeploy;
import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Bus;
import net.neoforged.bus.api.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Replaces the vanilla {@link DeathScreen} with our COD-style screen
 * the moment it is about to open.
 */
@EventBusSubscriber(modid = Redeploy.MODID, value = Dist.CLIENT, bus = Bus.GAME)
public class ClientEvents {

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof DeathScreen) {
            event.setNewScreen(new RedeployDeathScreen());
        }
    }
}
