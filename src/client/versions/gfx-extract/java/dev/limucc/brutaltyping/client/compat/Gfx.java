package dev.limucc.brutaltyping.client.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Thin graphics facade for MC 26.1+ — delegates 1:1 to {@link GuiGraphicsExtractor}. The facade exists so the
 * shared engine/GUI code compiles unchanged against every supported Minecraft version; older targets swap in
 * a {@code Gfx} that adapts the same calls to their era's {@code GuiGraphics}.
 */
public final class Gfx {

    private final GuiGraphicsExtractor g;
    private final Pose pose = new Pose();

    public Gfx(GuiGraphicsExtractor g) { this.g = g; }

    public Pose pose() { return pose; }

    public final class Pose {
        public void pushMatrix()                 { g.pose().pushMatrix(); }
        public void popMatrix()                  { g.pose().popMatrix(); }
        public void translate(float x, float y)  { g.pose().translate(x, y); }
        public void rotate(float radians)        { g.pose().rotate(radians); }
        public void scale(float s)               { g.pose().scale(s); }
    }

    public void fill(int x1, int y1, int x2, int y2, int color) { g.fill(x1, y1, x2, y2, color); }

    public void fillGradient(int x1, int y1, int x2, int y2, int from, int to) { g.fillGradient(x1, y1, x2, y2, from, to); }

    public void enableScissor(int x1, int y1, int x2, int y2) { g.enableScissor(x1, y1, x2, y2); }

    public void disableScissor() { g.disableScissor(); }

    public void text(Font font, String s, int x, int y, int color) { g.text(font, s, x, y, color); }

    public void text(Font font, String s, int x, int y, int color, boolean shadow) { g.text(font, s, x, y, color, shadow); }

    public void item(ItemStack stack, int x, int y) { g.item(stack, x, y); }
}
