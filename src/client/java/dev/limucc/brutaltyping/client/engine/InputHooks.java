package dev.limucc.brutaltyping.client.engine;

import dev.limucc.brutaltyping.client.config.BrutalConfig;
import dev.limucc.brutaltyping.client.config.BrutalConfigManager;
import dev.limucc.brutaltyping.client.gui.BrutalScreenCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * Version-independent half of the EditBox hooks. The per-version {@code EditBoxMixin} only unpacks its
 * version's event/parameter shape and forwards here. When a character is accepted we fire the amplifier;
 * backspace/delete fires the heavier "boom". The caret's on-screen position is estimated so effects burst
 * from where you're typing.
 */
public final class InputHooks {

    private InputHooks() {}

    public static void charTyped(EditBox box, int codepoint) {
        if (!shouldFire()) return;
        float[] caret = caret(box);
        EffectEngine.INSTANCE.onChar(codepoint, caret[0], caret[1], allCapsWord(box));
    }

    public static void keyPressed(EditBox box, int key) {
        if (key != GLFW.GLFW_KEY_BACKSPACE && key != GLFW.GLFW_KEY_DELETE) return;
        if (!shouldFire()) return;
        float[] caret = caret(box);
        EffectEngine.INSTANCE.onDelete(caret[0], caret[1]);
    }

    private static boolean shouldFire() {
        BrutalConfig c = BrutalConfigManager.get();
        if (!c.enabled) return false;
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) return false;
        if (screen instanceof BrutalScreenCore) return true;   // live preview on our own settings screen
        if (screen instanceof ChatScreen) return c.inChat;
        return c.inTextFields;
    }

    /** True when the word the caret sits in is ALL-CAPS (e.g. NOT, STOP) — at least 2 chars, no lowercase. */
    private static boolean allCapsWord(EditBox box) {
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

    private static float[] caret(EditBox box) {
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
