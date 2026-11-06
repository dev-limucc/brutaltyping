package dev.limucc.brutaltyping.client.mixin;

import dev.limucc.brutaltyping.client.compat.Gfx;
import dev.limucc.brutaltyping.client.engine.OverlayHooks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MC 26.2+: the whole in-game HUD moved from {@code Gui} to the new {@code Hud} class.
 * {@code Gui.extractRenderState} is now {@code (DeltaTracker, boolean, boolean)} and no longer
 * receives the graphics extractor, so we hook {@code Hud.extractRenderState(GuiGraphicsExtractor, DeltaTracker)}
 * instead (Gui calls it only while the HUD is shown; same moment the 26.1 hook ran).
 * The class keeps the name GuiMixin so brutaltyping.client.mixins.json stays era-agnostic.
 */
@Mixin(Hud.class)
public class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void brutaltyping$hudOverlay(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
        OverlayHooks.hud(new Gfx(g));
    }
}
