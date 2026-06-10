package dev.limucc.brutaltyping.client.gui;

import dev.limucc.brutaltyping.client.compat.Gfx;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** MC 1.21–1.21.8 glue: classic rendering + (x, y, button) mouse input. Logic in {@link BrutalScreenCore}. */
public class BrutalTypingScreen extends BrutalScreenCore {

    public BrutalTypingScreen(Screen parent) {
        super(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float a) {
        super.render(g, mouseX, mouseY, a);
        renderCore(new Gfx(g), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;   // let the preview EditBox grab clicks first
        if (button != 0) return false;
        return clickCore(mx, my);
    }
}
