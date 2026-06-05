package dev.limucc.brutaltyping.client.mixin;

import dev.limucc.brutaltyping.client.config.BrutalConfig;
import dev.limucc.brutaltyping.client.config.BrutalConfigManager;
import dev.limucc.brutaltyping.client.engine.EffectEngine;
import dev.limucc.brutaltyping.client.gui.BrutalTypingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks every text field. When a character is accepted we fire the amplifier; backspace/delete fires the
 * heavier "boom". The caret's on-screen position is estimated so effects burst from where you're typing.
 */
@Mixin(EditBox.class)
public class EditBoxMixin {

    @Inject(method = "charTyped", at = @At("RETURN"))
    private void brutaltyping$onCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;            // char wasn't accepted by this field
        if (!brutaltyping$shouldFire()) return;
        EditBox box = (EditBox) (Object) this;
        float[] caret = brutaltyping$caret(box);
        EffectEngine.INSTANCE.onChar(event.codepoint(), caret[0], caret[1], brutaltyping$allCapsWord(box));
    }

    /** True when the word the caret sits in is ALL-CAPS (e.g. NOT, STOP) — at least 2 chars, no lowercase. */
    private static boolean brutaltyping$allCapsWord(EditBox box) {
        String v = box.getValue();
        int cur = box.getCursorPosition();
        if (cur < 0) cur = 0;
        if (cur > v.length()) cur = v.length();
        int start = cur;
        while (start > 0 && !Character.isWhitespace(v.charAt(start - 1))) start--;
        String word = v.substring(start, cur);
        if (word.length() < 2) return false;
        boolean hasLetter = false;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (Character.isLetter(ch)) {
                hasLetter = true;
                if (Character.isLowerCase(ch)) return false;
            }
        }
        return hasLetter;
    }

    @Inject(method = "keyPressed", at = @At("RETURN"))
    private void brutaltyping$onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        int k = event.key();
        if (k != GLFW.GLFW_KEY_BACKSPACE && k != GLFW.GLFW_KEY_DELETE) return;
        if (!brutaltyping$shouldFire()) return;
        float[] caret = brutaltyping$caret((EditBox) (Object) this);
        EffectEngine.INSTANCE.onDelete(caret[0], caret[1]);
    }

    private static boolean brutaltyping$shouldFire() {
        BrutalConfig c = BrutalConfigManager.get();
        if (!c.enabled) return false;
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) return false;
        if (screen instanceof BrutalTypingScreen) return true;   // live preview on our own settings screen
        if (screen instanceof ChatScreen) return c.inChat;
        return c.inTextFields;
    }

    private static float[] brutaltyping$caret(EditBox box) {
        Font font = Minecraft.getInstance().font;
        String val = box.getValue();
        int cur = box.getCursorPosition();
        if (cur < 0) cur = 0;
        if (cur > val.length()) cur = val.length();
        int textW = font.width(val.substring(0, cur));
        int inner = Math.max(0, box.getInnerWidth());
        int caretX = box.getX() + 4 + Math.min(textW, inner);
        int maxX = box.getX() + box.getWidth() - 2;
        if (caretX > maxX) caretX = maxX;
        int caretY = box.getY() + box.getHeight() / 2;
        return new float[]{caretX, caretY};
    }
}
