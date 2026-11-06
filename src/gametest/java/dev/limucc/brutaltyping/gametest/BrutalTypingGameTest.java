package dev.limucc.brutaltyping.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.limucc.brutaltyping.client.compat.ScreenNav;
import dev.limucc.brutaltyping.client.config.BrutalConfigManager;
import dev.limucc.brutaltyping.client.engine.EffectEngine;
import dev.limucc.brutaltyping.client.engine.OverlayHooks;
import dev.limucc.brutaltyping.client.gui.BrutalTypingScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Era-agnostic runtime check for every Brutal Typing feature. Run with
 * {@code gradlew runClientGameTest "-Ptarget=<mc>"}. Screenshots land in run/screenshots.
 */
public class BrutalTypingGameTest implements FabricClientGameTest {

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("BT-GAMETEST FAIL: " + what);
        System.out.println("[BT-GAMETEST] PASS: " + what);
    }

    private static Screen screen(ClientGameTestContext ctx) {
        return ctx.computeOnClient(ScreenNav::current);
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        // 1. Force every mixin (GuiMixin/Hud, ScreenMixin, EditBoxMixin, ChatScreenMixin) to apply now.
        ctx.runOnClient(mc -> MixinEnvironment.getCurrentEnvironment().audit());
        check(true, "MixinEnvironment.audit() passed (all mixins applied)");

        ctx.runOnClient(mc -> {
            var c = BrutalConfigManager.get();
            c.enabled = true;
            c.inChat = true;
            c.inTextFields = true;
        });

        try (var world = ctx.worldBuilder().create()) {
            ctx.waitTicks(80);
            ctx.takeScreenshot("bt-01-world-baseline");

            // 2. HUD overlay path (GuiMixin): effects keep rendering with no screen open.
            int hudBefore = OverlayHooks.hudFrames;
            ctx.runOnClient(mc -> {
                for (int i = 0; i < 6; i++) {
                    EffectEngine.INSTANCE.onChar("BRUTAL".charAt(i), 220 + i * 14, 120, true);
                }
            });
            check(EffectEngine.INSTANCE.particleCount() > 0, "typing spawned particles (" + EffectEngine.INSTANCE.particleCount() + ")");
            ctx.waitTick();
            ctx.takeScreenshot("bt-02-hud-effects");
            ctx.waitTicks(80);
            check(OverlayHooks.hudFrames > hudBefore, "Hud mixin fired (frames +" + (OverlayHooks.hudFrames - hudBefore) + ")");
            check(EffectEngine.INSTANCE.particleCount() == 0, "HUD path decayed particles to 0");

            // 3. Chat: open with the real keybind, type, delete, send.
            ctx.getInput().pressKey(options -> options.keyChat);
            ctx.waitForScreen(ChatScreen.class);
            check(screen(ctx) instanceof ChatScreen, "chat screen opened via keybind");
            int headBefore = OverlayHooks.screenHeadFrames, tailBefore = OverlayHooks.screenTailFrames;
            int particlesBefore = EffectEngine.INSTANCE.particleCount();
            ctx.getInput().typeChars("BRUTAL typing NOT STOP");
            ctx.waitTick();
            check(EffectEngine.INSTANCE.combo() >= 10, "EditBox.charTyped mixin: combo=" + EffectEngine.INSTANCE.combo());
            check(EffectEngine.INSTANCE.heat() > 0.05f, "amplifier heat=" + EffectEngine.INSTANCE.heat());
            check(EffectEngine.INSTANCE.particleCount() > particlesBefore, "chat typing spawned particles");
            ctx.takeScreenshot("bt-03-chat-typing");
            check(OverlayHooks.screenHeadFrames > headBefore && OverlayHooks.screenTailFrames > tailBefore,
                    "Screen mixin HEAD+RETURN fired (head+" + (OverlayHooks.screenHeadFrames - headBefore)
                            + " tail+" + (OverlayHooks.screenTailFrames - tailBefore) + ")");

            int pBeforeDel = EffectEngine.INSTANCE.particleCount();
            ctx.getInput().pressKey(InputConstants.KEY_BACKSPACE);
            ctx.getInput().pressKey(InputConstants.KEY_BACKSPACE);
            ctx.waitTick();
            check(EffectEngine.INSTANCE.particleCount() > pBeforeDel, "EditBox.keyPressed mixin: backspace spawned debris");
            ctx.takeScreenshot("bt-04-chat-delete");

            // send-slam via real Enter
            ctx.getInput().pressKey(InputConstants.KEY_RETURN);
            ctx.waitTick();
            check(EffectEngine.INSTANCE.slamActive(), "ChatScreen.handleChatInput mixin: send-slam active");
            ctx.takeScreenshot("bt-05-send-slam");
            ctx.waitTicks(10);
            check(!(screen(ctx) instanceof ChatScreen), "chat closed after send; HUD keeps animating slam");
            ctx.takeScreenshot("bt-06-slam-on-hud");
            ctx.waitTicks(40);

            // 4. Settings screen (own UI, "Try typing here" box + tab clicks). 1280x720 so the 440px panel fits.
            ctx.getInput().resizeWindow(1280, 720);
            ctx.waitTicks(5);
            ctx.setScreen(() -> new BrutalTypingScreen(null));
            ctx.waitForScreen(BrutalTypingScreen.class);
            ctx.waitTicks(5);
            ctx.takeScreenshot("bt-07-settings-general");
            ctx.getInput().typeChars("TESTING");
            ctx.waitTick();
            ctx.takeScreenshot("bt-08-settings-preview-typing");

            double scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
            int w = ctx.computeOnClient(mc -> ScreenNav.current(mc).width);
            int panelLeft = w / 2 - 220;
            int tabW = 440 / 7;
            String[] names = {"general", "voice", "screen", "brutality", "amplifier", "exclusive", "info"};
            for (int i = 1; i < names.length; i++) {
                double cx = (panelLeft + i * tabW + tabW / 2.0) * scale;
                double cy = 33 * scale;
                ctx.getInput().setCursorPos(cx, cy);
                ctx.getInput().pressMouse(0);
                ctx.waitTicks(3);
                ctx.takeScreenshot("bt-09-tab-" + i + "-" + names[i]);
            }
            ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
            ctx.waitTicks(5);
            check(screen(ctx) == null, "settings screen closed with Escape");
        }
    }
}
