package com.mrkun114514.redeploy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * A Call of Duty style "YOU DIED / REDEPLOY" death overlay.
 *
 * <p><b>Rendering generation: 1.20.1</b> — this variant uses the legacy
 * {@code PoseStack} 3D matrix API ({@code pushPose/popPose}, 3-arg
 * {@code translate/scale}) instead of the {@code Matrix3x2fStack} introduced
 * in 1.21.6, and reads sounds via the pre-1.21.2 nullable registry
 * {@code get(ResourceLocation)} accessor.</p>
 *
 * <p>Vanilla {@code DeathScreen} is replaced by this screen. In normal mode the
 * player must <b>hold</b> the {@code [REDEPLOY]} button to respawn. In Hardcore
 * mode the screen offers <b>two</b> long-press choices: {@code [DETACH]}
 * (脱离机体 — respawn as a spectator) and {@code [EXTRACT]} (退出战区 — leave to
 * the title screen). The Hardcore screen is visually distinct (near-black
 * resting overlay + cold white title) from the normal red/navy one.</p>
 *
 * @author Mrkun114514
 */
public class RedeployDeathScreen extends Screen {

    // ---- tuning -----------------------------------------------------------
    /** Ticks (20/s) the screen waits before you are allowed to hold. ~2s. */
    private static final int READY_DELAY = 40;
    /** Ticks of continuous holding required to confirm. ~3s. */
    private static final int HOLD_REQUIRED = 60;
    /** Play a tick sound every N ticks of holding. */
    private static final int TICK_EVERY = 8;

    // Intro animation (wall-clock based, independent of the mod's tick timers).
    /** ms the solid-red flash lasts before it starts fading to the resting overlay. */
    private static final long INTRO_RED = 1200;
    /** ms the red->resting fade + GUI fade-in takes. */
    private static final long INTRO_FADE = 1000;

    // Overlay colours as 0xAARRGGBB. Muted + semi-transparent so it is not blinding.
    private static final int RED_FLASH = 0x99D23B2E;       // soft red flash (both modes)
    private static final int NAVY_REST = 0xDD0A0E2A;       // normal resting: dark navy
    private static final int DARK_REST = 0xDD0A0A0A;        // hardcore resting: near-black

    // Title colour: normal = red, hardcore = cold white ("game over" feel).
    private static final int TITLE_NORMAL = 0xFFD23B2E;
    private static final int TITLE_HARDCORE = 0xFFE8E8E8;

    // Vanilla sounds are used so the mod ships with zero binary assets.
    private static final ResourceLocation TICK_SOUND =
            new ResourceLocation("minecraft:block.comparator.click");   // mechanical "tick"
    private static final ResourceLocation CONFIRM_SOUND =
            new ResourceLocation("minecraft:entity.player.levelup");      // success jingle

    // Button ids.
    private static final int BTN_NONE = 0;
    private static final int BTN_REDEPLOY = 1;   // normal mode
    private static final int BTN_SPECTATE = 1;   // hardcore: 脱离机体 (same slot, hardcore-only)
    private static final int BTN_QUIT = 2;       // hardcore: 退出战区

    // ---- state -------------------------------------------------------------
    private int ticksOpen = 0;
    private int holdBtn = BTN_NONE;   // which button is currently being held
    private int holdTicks = 0;
    private boolean done = false;
    private final boolean hardcore;

    private double lastMx = 0, lastMy = 0;
    private int btnX, btnY, btnW, btnH;          // primary button (normal) / top (hardcore)
    private int btn2X, btn2Y, btn2W, btn2H;     // hardcore bottom button (0-size when unused)
    private long openedAt;
    /** Set when ESC is used to open the pause menu, so {@link #removed()} won't
     *  mistake the open-pause transition for a real close and resolve the player. */
    private boolean goingToPause = false;

    public RedeployDeathScreen() {
        this(false);
    }

    public RedeployDeathScreen(boolean hardcore) {
        super(Component.literal("ReDeploy"));
        this.hardcore = hardcore;
    }

    @Override
    protected void init() {
        super.init();
        this.btnH = 56;
        // Layout is computed from the title so title/subtitle can never overlap
        // the buttons, even on small screens.
        int titleY = (int) (this.height * 0.28);
        int subY = titleY + 64;
        // Hardcore buttons are the SAME size as the normal REDEPLOY button.
        this.btnW = Math.min(420, this.width - 80);
        this.btnX = (this.width - this.btnW) / 2;
        // Both normal and Hardcore now show TWO stacked buttons
        // (respawn / spectate on top, quit-to-title on the bottom).
        // Generous spacing to keep both buttons clearly clickable.
        this.btnY = subY + 96;
        this.btn2X = this.btnX;
        this.btn2Y = this.btnY + this.btnH + 24;
        this.btn2W = this.btnW;
        this.openedAt = System.currentTimeMillis();
    }

    /** Real-time feel: the world keeps rendering behind the dim overlay. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (done) return;
        Minecraft mc = Minecraft.getInstance();
        // Server-side respawn (e.g. an instant-respawn / auto-revive plugin) makes the
        // player alive again. Proactively close the GUI instead of force-triggering
        // our own respawn — the server already did the job.
        if (mc.player != null && !mc.player.isDeadOrDying()) {
            mc.setScreen(null);
            return;
        }
        ticksOpen++;

        // Cancel the hold if the cursor drifted off the held button.
        if (holdBtn != BTN_NONE && !insideBtn(holdBtn, lastMx, lastMy)) {
            holdBtn = BTN_NONE;
            holdTicks = 0;
        }

        if (holdBtn != BTN_NONE && ready()) {
            holdTicks++;
            if (holdTicks % TICK_EVERY == 0) {
                float t = holdTicks / (float) HOLD_REQUIRED;   // 0..1
                playSound(TICK_SOUND, 1.0f + t * 1.2f);     // rising pitch = tension
            }
            if (holdTicks >= HOLD_REQUIRED) {
                complete(holdBtn);
            }
        }
    }

    /** Fire the action for the chosen button. */
    private void complete(int btn) {
        if (btn == BTN_QUIT) {
            quitToTitle();
        } else {
            doRespawn();   // normal respawn, or hardcore spectate (server forces spectator)
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        long elapsed = System.currentTimeMillis() - openedAt;
        // 0 during the red flash, ramps to 1 across the fade + GUI fade-in.
        float t = clamp((float) (elapsed - INTRO_RED) / INTRO_FADE, 0f, 1f);
        int rest = hardcore ? DARK_REST : NAVY_REST;
        int overlay = lerpColor(RED_FLASH, rest, t);
        gui.fill(0, 0, this.width, this.height, overlay);

        // Subtle dark vignette top & bottom for the COD vibe (fades in with GUI).
        gui.fillGradient(0, 0, this.width, this.height / 4,
                withAlpha(0x33000000, t), 0x00000000);
        gui.fillGradient(0, (this.height * 3) / 4, this.width, this.height,
                0x00000000, withAlpha(0x33000000, t));

        super.render(gui, mouseX, mouseY, partial);

        // During the pure-red flash there is no GUI yet.
        if (t <= 0.001f) return;

        int cx = this.width / 2;
        int titleY = (int) (this.height * 0.28);
        int subY = titleY + 64;

        // --- Title (large) ---
        Component title = Component.translatable(
                hardcore ? "screen.redeploy.title.hardcore" : "screen.redeploy.title");
        gui.pose().pushPose();
        gui.pose().translate(cx, (float) titleY, 0);
        gui.pose().scale(3.0f, 3.0f, 1.0f);
        gui.drawCenteredString(this.font, title, 0, 0,
                withAlpha(hardcore ? TITLE_HARDCORE : TITLE_NORMAL, t));
        gui.pose().popPose();

        // --- Subtitle (small, grey) ---
        Component sub = Component.translatable(
                hardcore ? "screen.redeploy.subtitle.hardcore" : "screen.redeploy.subtitle");
        gui.drawCenteredString(this.font, sub, cx, subY, withAlpha(0xFF9A9A9A, t));

        // --- Hint above the buttons ---
        Component hint = hint();
        gui.drawCenteredString(this.font, hint, cx, this.btnY - 26, withAlpha(0xFFBFBFBF, t));

        // --- Buttons ---
        if (hardcore) {
            drawButton(gui, BTN_SPECTATE, btnX, btnY, btnW, btnH, t, "screen.redeploy.button.spectate");
            drawButton(gui, BTN_QUIT, btn2X, btn2Y, btn2W, btn2H, t, "screen.redeploy.button.quit");
        } else {
            drawButton(gui, BTN_REDEPLOY, btnX, btnY, btnW, btnH, t, "screen.redeploy.button");
            drawButton(gui, BTN_QUIT, btn2X, btn2Y, btn2W, btn2H, t, "screen.redeploy.button.menu");
        }

        // --- Author watermark ---
        Component credit = Component.literal("ReDeploy · Mrkun114514");
        int cw = this.font.width(credit);
        gui.drawString(this.font, credit, this.width - cw - 8, this.height - 14,
                withAlpha(0x88909090, t), false);
    }

    /** Draw one button with hover / hold feedback and a full-width progress fill. */
    private void drawButton(GuiGraphics gui, int id, int x, int y, int w, int h,
                            float t, String labelKey) {
        if (w <= 0) return;
        boolean hovering = ready() && insideBtn(id, lastMx, lastMy) && holdBtn == BTN_NONE && !done;
        boolean holdingThis = holdBtn == id;
        int border;
        if (holdingThis) {
            border = 0xFFE8B339;                                  // gold while holding
        } else if (hovering) {
            border = 0xFFC8A24A;                                  // brighter gold on hover
        } else {
            border = 0xFF5A5A5A;                                  // idle grey
        }
        int fillBase = hovering ? 0x88000000 : 0x66000000;
        gui.fill(x, y, x + w, y + h, withAlpha(fillBase, t));
        gui.renderOutline(x, y, w, h, withAlpha(border, t));

        // Progress fills the WHOLE button width (COD style), not just a 5px strip.
        float p = holdingThis ? Math.max(0f, Math.min(1f, holdTicks / (float) HOLD_REQUIRED)) : 0f;
        if (p > 0f) {
            int fillW = (int) (w * p);
            gui.fill(x, y, x + fillW, y + h, withAlpha(0x55E8B339, t));
        }

        Component label = Component.translatable(labelKey);
        gui.drawCenteredString(this.font, label, x + w / 2, y + h / 2 - 4,
                withAlpha(0xFFFFFFFF, t));
    }

    // ---- input -------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMx = mouseX;
        lastMy = mouseY;
        if (canStartHold(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        lastMx = mouseX;
        lastMy = mouseY;
        holdBtn = BTN_NONE;
        holdTicks = 0;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        lastMx = mouseX;
        lastMy = mouseY;
        if (holdBtn != BTN_NONE && !insideBtn(holdBtn, mouseX, mouseY)) {
            holdBtn = BTN_NONE;
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

    /**
     * ESC opens the vanilla pause menu (the standard {@code PauseScreen(false)}),
     * giving the player a backdoor to quit to title / open options. From the pause
     * menu, "Quit to Title" / "Disconnect" is the real escape — a dead player
     * who picks "Back to Game" simply returns to the dead world (you cannot
     * un-die), which re-shows this screen; that is expected Hardcore behaviour.
     * {@code goingToPause} tells {@link #removed()} that this transition is the
     * intentional ESC, so it must NOT resolve the player.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            goingToPause = true;
            Minecraft.getInstance().setScreen(new PauseScreen(false));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Safety net for genuine external closes (another mod, MC internals). Only the
     * normal mode resolves the player (respawn) — and only while they are still
     * dead. Hardcore mode performs NO automatic action here: the two buttons are
     * the only ways out (spectate / quit to title), and force-quitting on every
     * close would yank the player to the title screen instantly. Server-driven
     * respawns are handled by {@link #tick()} closing the GUI.
     */
    @Override
    public void removed() {
        if (!goingToPause && !done) {
            Minecraft mc = Minecraft.getInstance();
            if (!hardcore && mc.player != null && mc.player.isDeadOrDying()) {
                doRespawn();
            }
        }
        super.removed();
    }

    // ---- helpers -----------------------------------------------------------

    private boolean ready() {
        return ticksOpen >= READY_DELAY;
    }

    private boolean insideBtn(int id, double mx, double my) {
        if (id == BTN_REDEPLOY || id == BTN_SPECTATE) {
            return mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;
        } else if (id == BTN_QUIT) {
            return mx >= btn2X && mx <= btn2X + btn2W && my >= btn2Y && my <= btn2Y + btn2H;
        }
        return false;
    }

    private boolean canStartHold(double mx, double my) {
        if (!ready() || done) return false;
        if (hardcore) {
            if (insideBtn(BTN_SPECTATE, mx, my)) { holdBtn = BTN_SPECTATE; holdTicks = 0; return true; }
            if (insideBtn(BTN_QUIT, mx, my))      { holdBtn = BTN_QUIT;      holdTicks = 0; return true; }
            return false;
        } else {
            if (insideBtn(BTN_REDEPLOY, mx, my)) { holdBtn = BTN_REDEPLOY; holdTicks = 0; return true; }
            if (insideBtn(BTN_QUIT, mx, my))      { holdBtn = BTN_QUIT;      holdTicks = 0; return true; }
            return false;
        }
    }

    private Component hint() {
        String base;
        if (!ready()) base = "screen.redeploy.hint.wait";
        else if (holdBtn != BTN_NONE && holdTicks < HOLD_REQUIRED) base = "screen.redeploy.hint.holding";
        else base = "screen.redeploy.hint";
        return Component.translatable(hardcore ? base + ".hardcore" : base);
    }

    private void doRespawn() {
        if (done) return;
        done = true;
        playSound(CONFIRM_SOUND, 1.0f);
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && player.isDeadOrDying()) {
            player.respawn();   // in Hardcore the server respawns the player as a spectator
        }
        mc.setScreen(null);
    }

    /** Hardcore: leaving to the title screen (退出战区). Same as vanilla "Title screen". */
    private void quitToTitle() {
        if (done) return;
        done = true;
        playSound(CONFIRM_SOUND, 1.0f);
        Minecraft mc = Minecraft.getInstance();
        // clearLevel() saves the integrated server (singleplayer) and transitions
        // to the title screen internally — same as vanilla DeathScreen hardcore.
        // Do NOT call setScreen() after it, or the async save will race with the
        // screen transition and corrupt the world save.
        mc.clearLevel();
    }

    private void playSound(ResourceLocation id, float pitch) {
        // 1.20.1 registry API: get(ResourceLocation) returns a nullable value.
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(id);
        if (event == null) return;
        SoundManager mgr = Minecraft.getInstance().getSoundManager();
        mgr.play(SimpleSoundInstance.forUI(event, pitch));
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int lerpColor(int a, int b, float t) {
        t = clamp(t, 0f, 1f);
        int ar = (a >> 24) & 0xFF, ag = (a >> 16) & 0xFF, ab = (a >> 8) & 0xFF, aa = a & 0xFF;
        int br = (b >> 24) & 0xFF, bg = (b >> 16) & 0xFF, bb = (b >> 8) & 0xFF, ba = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        int al = (int) (aa + (ba - aa) * t);
        return (r << 24) | (g << 16) | (bl << 8) | al;
    }

    private static int withAlpha(int rgb, float a) {
        int al = (int) (clamp(a, 0f, 1f) * 255);
        return (rgb & 0x00FFFFFF) | (al << 24);
    }
}
