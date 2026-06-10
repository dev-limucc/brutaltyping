package dev.limucc.brutaltyping.client.compat;

import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Graphics facade for MC 1.21–1.21.5 — the {@code GuiGraphics} era with a 3D {@code PoseStack}. 2D calls from
 * the shared code are adapted: translate gets z=0, rotate spins around the Z axis, scale keeps z=1. Same
 * public surface as the 26.1+ variant so the shared engine/GUI code compiles unchanged.
 */
public final class Gfx {

    private final GuiGraphics g;
    private final Pose pose = new Pose();

    public Gfx(GuiGraphics g) { this.g = g; }

    public Pose pose() { return pose; }

    public final class Pose {
        public void pushMatrix()                 { g.pose().pushPose(); }
        public void popMatrix()                  { g.pose().popPose(); }
        public void translate(float x, float y)  { g.pose().translate(x, y, 0f); }
        public void rotate(float radians)        { g.pose().mulPose(Axis.ZP.rotation(radians)); }
        public void scale(float s)               { g.pose().scale(s, s, 1f); }
    }

    public void fill(int x1, int y1, int x2, int y2, int color) { g.fill(x1, y1, x2, y2, color); }

    public void fillGradient(int x1, int y1, int x2, int y2, int from, int to) { g.fillGradient(x1, y1, x2, y2, from, to); }

    public void enableScissor(int x1, int y1, int x2, int y2) { g.enableScissor(x1, y1, x2, y2); }

    public void disableScissor() { g.disableScissor(); }

    public void text(Font font, String s, int x, int y, int color) { g.drawString(font, s, x, y, color); }

    public void text(Font font, String s, int x, int y, int color, boolean shadow) { g.drawString(font, s, x, y, color, shadow); }

    public void item(ItemStack stack, int x, int y) { g.renderItem(stack, x, y); }
}
