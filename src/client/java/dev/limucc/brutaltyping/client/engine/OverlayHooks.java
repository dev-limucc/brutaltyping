package dev.limucc.brutaltyping.client.engine;

import dev.limucc.brutaltyping.client.compat.ScreenNav;
import dev.limucc.brutaltyping.client.compat.Gfx;
import dev.limucc.brutaltyping.client.config.BrutalConfig;
import dev.limucc.brutaltyping.client.config.BrutalConfigManager;
import dev.limucc.brutaltyping.client.gui.BrutalScreenCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Version-independent half of the Screen/HUD render hooks. The per-version mixins only adapt the injection
 * point + graphics type and forward here. {@code screenHead} pushes a shake translation (so the entire
 * screen — chatbox included — rattles); {@code screenTail} draws the particle overlay on top of everything,
 * then pops. One push, one pop, every frame, so the matrix stack stays balanced even when shake/effects are
 * off. {@code hud} renders the overlay on the in-game HUD when no screen is open, so effects (and the
 * send-slam) keep playing for a moment after the chat closes — exactly one path advances the frame clock.
 */
public final class OverlayHooks {

    private OverlayHooks() {}

    public static void screenHead(Screen self, Gfx g) {
        EffectEngine engine = EffectEngine.INSTANCE;
        engine.beginFrame();

        float fx = 0f, fy = 0f;
        if (shake(self)) {
            float factor = (self instanceof BrutalScreenCore) ? 0.4f : 1f; // keep settings UI usable
            fx = engine.shakeX() * factor;
            fy = engine.shakeY() * factor;
        }
        g.pose().pushMatrix();
        g.pose().translate(fx, fy);
    }

    public static void screenTail(Screen self, Gfx g) {
        EffectEngine.INSTANCE.renderOverlay(g, self.width, self.height, Minecraft.getInstance().font);
        g.pose().popMatrix();
    }

    public static void hud(Gfx g) {
        Minecraft mc = Minecraft.getInstance();
        if (ScreenNav.current(mc) != null) return;           // screens handled by the Screen mixin
        if (!BrutalConfigManager.get().enabled) return;
        EffectEngine.INSTANCE.beginFrame();
        EffectEngine.INSTANCE.renderOverlay(g,
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), mc.font);
    }

    private static boolean shake(Screen screen) {
        BrutalConfig c = BrutalConfigManager.get();
        if (!c.enabled) return false;
        if (screen instanceof BrutalScreenCore) return true; // damped demo shake while configuring
        return switch (c.shakeTarget) {
            case OFF -> false;
            case SCREEN -> true;
            case CHAT_ONLY -> screen instanceof ChatScreen;
        };
    }
}
