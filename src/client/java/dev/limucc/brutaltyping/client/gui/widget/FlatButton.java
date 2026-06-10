package dev.limucc.brutaltyping.client.gui.widget;

import net.minecraft.client.gui.Font;
import dev.limucc.brutaltyping.client.compat.Gfx;

/**
 * Limucc-style flat button (from Trashventory): a dark rounded-feel rectangle with a 1px top highlight, an
 * accent-blue hover fill, and centered text. Drawn by the owning screen and hit-tested in mouseClicked.
 */
public class FlatButton {

    public int x, y, w, h;
    public String label;

    private static final int BG        = 0xFF26262B;
    private static final int BG_HOVER  = 0xFF3A6EA5;   // accent blue
    private static final int BG_OFF    = 0xFF1C1C20;
    private static final int BG_ACTIVE = 0xFF2E5A86;   // selected tab (dim accent)
    private static final int TEXT      = 0xFFFFFFFF;
    private static final int TEXT_OFF  = 0xFF6A6A70;

    public FlatButton(int x, int y, int w, int h, String label) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.label = label;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void render(Gfx g, Font font, int mouseX, int mouseY, boolean enabled) {
        render(g, font, mouseX, mouseY, enabled, false);
    }

    public void render(Gfx g, Font font, int mouseX, int mouseY, boolean enabled, boolean active) {
        boolean hovered = enabled && contains(mouseX, mouseY);
        int bg = !enabled ? BG_OFF : (hovered ? BG_HOVER : (active ? BG_ACTIVE : BG));
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, 0x22FFFFFF);            // top highlight
        g.fill(x, y + h - 1, x + w, y + h, 0x44000000);    // bottom shade
        int tw = font.width(label);
        g.text(font, label, x + (w - tw) / 2, y + (h - 8) / 2, enabled ? TEXT : TEXT_OFF);
    }
}
