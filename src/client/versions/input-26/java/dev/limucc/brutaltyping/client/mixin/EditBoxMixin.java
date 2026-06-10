package dev.limucc.brutaltyping.client.mixin;

import dev.limucc.brutaltyping.client.engine.InputHooks;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MC 26.1+: hooks every text field via the event-record input API. */
@Mixin(EditBox.class)
public class EditBoxMixin {

    @Inject(method = "charTyped", at = @At("RETURN"))
    private void brutaltyping$onCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;            // char wasn't accepted by this field
        InputHooks.charTyped((EditBox) (Object) this, event.codepoint());
    }

    @Inject(method = "keyPressed", at = @At("RETURN"))
    private void brutaltyping$onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        InputHooks.keyPressed((EditBox) (Object) this, event.key());
    }
}
