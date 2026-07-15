package com.mrkun114514.redeploy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.Optional;

/**
 * A Call of Duty style "YOU DIED / REDEPLOY" death overlay.
 *
 * <p>Vanilla {@code DeathScreen} is replaced by this screen (see {@link ClientEvents}).
 * The player must <b>hold</b> the {@code [REDEPLOY]} button continuously for a few
 * seconds to respawn. A ticking confirmation sound plays while holding, and a
 * distinct confirm sound fires the moment redeploy completes.</p>
 *
 * @author Mrkun114514
 */
public class RedeployDeathScreen extends Screen {

    // ---- tuning -----------------------------------------------------------
    /** Ticks (20/s) the screen waits before you are allowed to hold. ~2s. */
    private static final int READY_DELAY = 40;
    /** Ticks of continuous holding required to redeploy. ~3s. */
    private static final int HOLD_REQUIRED = 60;
    /** Play a tick sound every N ticks of holding. */
    private static final int TICK_EVERY = 8;

    // Vanilla sounds are used so the mod ships with zero binary assets.
    private static final ResourceLocation TICK_SOUND =
            ResourceLocation.parse("minecraft:block.comparator.click");   // mechanical "tick"
    private static final ResourceLocation CONFIRM_SOUND =
            ResourceLocation.parse("minecraft:entity.player.levelup");      // success jingle

    // ---- state -------------------------------------------------------------
    private int ticksOpen = 0;
    private boolean holding = false;
    private int holdTicks = 0;
    private boolean done = false;

    private double lastMx = 0, lastMy = 0;
    private int btnX, btnY, btnW, btnH;

    public RedeployDeathScreen() {
        super(Component.literal("ReDeploy"));
    }

    @Override
    protected void init() {
        super.init();
        this.btnW = Math.min(420, this.width - 80);
        this.btnH = 56;
        this.btnX = (this.width - this.btnW) / 2;
        this.btnY = this.height - this.btnH - 90;
    }

    /** Real-time feel: the world keeps rendering behind the dim overlay. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (done) return;
        ticksOpen++;

        // Cancel the hold if the cursor drifted off the button.
        if (holding && !insideBtn(lastMx, lastMy)) {
            holding = false;
            holdTicks = 0;
        }

        if (holding && ready()) {
            holdTicks++;
            if (holdTicks % TICK_EVERY == 0) {
                float t = holdTicks / (float) HOLD_REQUIRED;   // 0..1
                playSound(TICK_SOUND, 1.0f + t * 1.2f);     // rising pitch = tension
            }
            if (holdTicks >= HOLD_REQUIRED) {
                doRespawn();
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        // Dim the world behind the overlay.
        gui.fill(0, 0, this.width, this.height, 0xCC000000);
        // Subtle dark vignette top & bottom for the COD vibe.
        gui.fillGradient(0, 0, this.width, this.height / 4, 0x33000000, 0x00000000);
        gui.fillGradient(0, (this.height * 3) / 4, this.width, this.height, 0x00000000, 0x33000000);

        super.render(gui, mouseX, mouseY, partial);

        int cx = this.width / 2;

        // --- Title (large, red) ---
        Component title = Component.translatable("screen.redeploy.title");   // 你已死亡
        gui.pose().pushMatrix();
        gui.pose().translate(cx, (float) (this.height * 0.30));
        gui.pose().scale(3.0f, 3.0f);
        gui.drawCenteredString(this.font, title, 0, 0, 0xFFD23B2E);
        gui.pose().popMatrix();

        // --- Subtitle (small, grey) ---
        Component sub = Component.translatable("screen.redeploy.subtitle");  // 重新部署 / REDEPLOY
        gui.drawCenteredString(this.font, sub, cx, (int) (this.height * 0.30) + 56, 0xFF9A9A9A);

        // --- Hint above the button ---
        Component hint = hint();
        gui.drawCenteredString(this.font, hint, cx, this.btnY - 26, 0xFFBFBFBF);

        // --- Redeploy button ---
        int border = holding ? 0xFFE8B339 : 0xFF5A5A5A;
        gui.fill(this.btnX, this.btnY, this.btnX + this.btnW, this.btnY + this.btnH, 0x66000000);
        gui.renderOutline(this.btnX, this.btnY, this.btnW, this.btnH, border);

        float p = Math.max(0f, Math.min(1f, holdTicks / (float) HOLD_REQUIRED));
        if (p > 0f) {
            int fillW = (int) (this.btnW * p);
            gui.fill(this.btnX, this.btnY + this.btnH - 5, this.btnX + fillW, this.btnY + this.btnH, 0xFFE8B339);
        }

        Component label = Component.translatable("screen.redeploy.button");  // 重新部署 / REDEPLOY
        gui.drawCenteredString(this.font, label, cx, this.btnY + this.btnH / 2 - 4, 0xFFFFFFFF);

        // --- Author watermark ---
        Component credit = Component.literal("ReDeploy · Mrkun114514");
        int cw = this.font.width(credit);
        gui.drawString(this.font, credit, this.width - cw - 8, this.height - 14, 0x88909090, false);
    }

    // ---- input -------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMx = mouseX;
        lastMy = mouseY;
        if (canStartHold(mouseX, mouseY)) {
            holding = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        lastMx = mouseX;
        lastMy = mouseY;
        holding = false;
        holdTicks = 0;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        lastMx = mouseX;
        lastMy = mouseY;
        if (holding && !insideBtn(mouseX, mouseY)) {
            holding = false;
            holdTicks = 0;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        lastMx = mouseX;
        lastMy = mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    /** Consume ESC so the screen cannot be closed without redeploying. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) return true; // ESC
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---- helpers -----------------------------------------------------------

    private boolean ready() {
        return ticksOpen >= READY_DELAY;
    }

    private boolean insideBtn(double mx, double my) {
        return mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;
    }

    private boolean canStartHold(double mx, double my) {
        return ready() && insideBtn(mx, my) && !done;
    }

    private Component hint() {
        if (!ready()) return Component.translatable("screen.redeploy.hint.wait");
        if (holding && holdTicks < HOLD_REQUIRED) return Component.translatable("screen.redeploy.hint.holding");
        return Component.translatable("screen.redeploy.hint");
    }

    private void doRespawn() {
        if (done) return;
        done = true;
        playSound(CONFIRM_SOUND, 1.0f);
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && player.isDeadOrDying()) {
            player.respawn();
        }
        mc.setScreen(null);
    }

    private void playSound(ResourceLocation id, float pitch) {
        Optional<net.minecraft.core.Holder.Reference<SoundEvent>> ref = BuiltInRegistries.SOUND_EVENT.get(id);
        if (ref.isEmpty()) return;
        SoundEvent event = ref.get().value();
        SoundManager mgr = Minecraft.getInstance().getSoundManager();
        mgr.play(SimpleSoundInstance.forUI(event, pitch));
    }
}
