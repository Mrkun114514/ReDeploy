package com.mrkun114514.redeploy;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * ReDeploy — a GUI-only mod that replaces the vanilla death screen
 * with a Call of Duty style "YOU DIED / REDEPLOY" overlay.
 *
 * @author Mrkun114514
 */
@Mod(Redeploy.MODID)
public class Redeploy {
    public static final String MODID = "redeploy";
    public static final String MOD_NAME = "ReDeploy";

    public Redeploy(IEventBus modEventBus) {
        // GUI-only mod: all behaviour lives in the client-side
        // screen-replacement subscriber (see client.ClientEvents).
    }
}
