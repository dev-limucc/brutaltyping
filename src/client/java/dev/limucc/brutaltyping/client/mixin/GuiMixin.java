package dev.limucc.brutaltyping.client.mixin;

import dev.limucc.brutaltyping.client.config.BrutalConfigManager;
import dev.limucc.brutaltyping.client.engine.EffectEngine;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the particle overlay on the in-game HUD when no screen is open, so effects (and the send-slam) keep
 * playing for a moment after the chat closes. When a screen IS open, {@link ScreenMixin} handles it instead, so
 * exactly one path advances the frame clock per frame.
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void brutaltyping$hudOverlay(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;                       // screens handled by ScreenMixin
        if (!BrutalConfigManager.get().enabled) return;
        EffectEngine.INSTANCE.beginFrame();
        EffectEngine.INSTANCE.renderOverlay(g,
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), mc.font);
    }
}
