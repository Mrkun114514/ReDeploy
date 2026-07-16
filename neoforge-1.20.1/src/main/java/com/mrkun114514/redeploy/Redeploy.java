package com.mrkun114514.redeploy;

import com.mrkun114514.redeploy.client.ClientEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * ReDeploy — a GUI-only mod that replaces the vanilla death screen with a
 * Call of Duty style "YOU DIED / REDEPLOY" overlay.
 *
 * <p><b>NeoForge 1.20.1</b> is the ForgeGradle-generation fork and still uses
 * the {@code net.minecraftforge.*} package names, so this milestone mirrors the
 * classic Forge 1.20.1 entrypoint style.</p>
 *
 * @author Mrkun114514
 */
@Mod(Redeploy.MODID)
public class Redeploy {
    public static final String MODID = "redeploy";

    public Redeploy() {
        // GUI-only client mod: only wire up the client-side screen replacement
        // when running on a physical client. Referencing ClientEvents inside the
        // dist guard keeps client-only classes off the dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEvents.register();
        }
    }
}
