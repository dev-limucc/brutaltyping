package dev.limucc.brutaltyping.client.mixin;

import dev.limucc.brutaltyping.client.config.BrutalConfig;
import dev.limucc.brutaltyping.client.config.BrutalConfigManager;
import dev.limucc.brutaltyping.client.engine.EffectEngine;
import dev.limucc.brutaltyping.client.gui.BrutalTypingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps the whole-screen render. At HEAD we push a shake translation (so the entire screen — chatbox
 * included — rattles); at RETURN we draw the particle overlay on top of everything, then pop. One push, one
 * pop, every frame, so the matrix stack stays balanced even when shake/effects are off.
 */
@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
    private void brutaltyping$shakeHead(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        EffectEngine engine = EffectEngine.INSTANCE;
        engine.beginFrame();

        float fx = 0f, fy = 0f;
        if (brutaltyping$shake(self)) {
            float factor = (self instanceof BrutalTypingScreen) ? 0.4f : 1f; // keep settings UI usable
            fx = engine.shakeX() * factor;
            fy = engine.shakeY() * factor;
        }
        g.pose().pushMatrix();
        g.pose().translate(fx, fy);
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("RETURN"))
    private void brutaltyping$overlayTail(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        EffectEngine.INSTANCE.renderOverlay(g, self.width, self.height, Minecraft.getInstance().font);
        g.pose().popMatrix();
    }

    private static boolean brutaltyping$shake(Screen screen) {
        BrutalConfig c = BrutalConfigManager.get();
        if (!c.enabled) return false;
        if (screen instanceof BrutalTypingScreen) return true; // damped demo shake while configuring
        return switch (c.shakeTarget) {
            case OFF -> false;
            case SCREEN -> true;
            case CHAT_ONLY -> screen instanceof ChatScreen;
        };
    }
}
