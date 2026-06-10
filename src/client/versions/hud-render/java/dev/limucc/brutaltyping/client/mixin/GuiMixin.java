package dev.limucc.brutaltyping.client.mixin;

import dev.limucc.brutaltyping.client.compat.Gfx;
import dev.limucc.brutaltyping.client.engine.OverlayHooks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MC 1.21–1.21.11: renders the particle overlay on the in-game HUD when no screen is open. */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void brutaltyping$hudOverlay(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
        OverlayHooks.hud(new Gfx(g));
    }
}
