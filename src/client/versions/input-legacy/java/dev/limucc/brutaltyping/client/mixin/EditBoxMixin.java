package dev.limucc.brutaltyping.client.mixin;

import dev.limucc.brutaltyping.client.engine.InputHooks;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MC 1.21–1.21.8: hooks every text field via the classic (char, modifiers)/(key, scancode, modifiers) API. */
@Mixin(EditBox.class)
public class EditBoxMixin {

    @Inject(method = "charTyped", at = @At("RETURN"))
    private void brutaltyping$onCharTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;            // char wasn't accepted by this field
        InputHooks.charTyped((EditBox) (Object) this, chr);
    }

    @Inject(method = "keyPressed", at = @At("RETURN"))
    private void brutaltyping$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        InputHooks.keyPressed((EditBox) (Object) this, keyCode);
    }
}
