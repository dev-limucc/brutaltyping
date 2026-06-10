package dev.limucc.brutaltyping.client.gui;

import dev.limucc.brutaltyping.client.compat.Gfx;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

/** MC 26.1+ glue: extract-render-state rendering + event-record mouse input. Logic in {@link BrutalScreenCore}. */
public class BrutalTypingScreen extends BrutalScreenCore {

    public BrutalTypingScreen(Screen parent) {
        super(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        renderCore(new Gfx(g), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;   // let the preview EditBox grab clicks first
        if (event.button() != 0) return false;
        return clickCore(event.x(), event.y());
    }
}
