package dev.limucc.brutaltyping.client.mixin;

import dev.limucc.brutaltyping.client.compat.Gfx;
import dev.limucc.brutaltyping.client.engine.OverlayHooks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MC 26.1+: wraps the whole-screen render-state extraction. Shake at HEAD, overlay + pop at RETURN. */
@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
    private void brutaltyping$shakeHead(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        OverlayHooks.screenHead((Screen) (Object) this, new Gfx(g));
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("RETURN"))
    private void brutaltyping$overlayTail(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        OverlayHooks.screenTail((Screen) (Object) this, new Gfx(g));
    }
}
