package dev.limucc.brutaltyping.client.engine;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * One screen-space particle. Everything is hand-drawn with {@code g.fill}/{@code g.text} and the JOML 2D pose
 * stack (no world particles, no texture atlas) so it works inside any GUI. The {@link Kind} decides how it
 * looks; motion is shared. Styled after Minecraft fire (orange/gold), soul fire (cyan) and TNT (flash +
 * shockwave + smoke + debris).
 */
public class Particle {

    public enum Kind { GLYPH, SPARK, FLAME, SOUL, SMOKE, DEBRIS, SHOCKWAVE, FLASH, CRIT, ENCHANT, FIREWORK, ITEM }

    public final Kind kind;
    public float x, y, vx, vy, ay;     // position / velocity / vertical accel (gravity or buoyancy)
    public float drag = 0f;            // velocity damping per second (0 = none)
    public float life = 0f, maxLife = 0.6f;
    public float size = 1f;            // meaning depends on kind (scale, radius, or flash strength)
    public float rot = 0f, vrot = 0f;  // radians
    public int color = 0xFFFFFFFF;
    public String glyph = "";          // for GLYPH
    public ItemStack stack;            // for ITEM (falling weapon textures)
    public float seed = 0f;            // per-particle phase for flicker/twinkle

    public Particle(Kind kind) { this.kind = kind; }

    public boolean dead() { return life >= maxLife; }

    public void update(float dt) {
        life += dt;
        vy += ay * dt;
        if (drag > 0f) {
            float d = Math.max(0f, 1f - drag * dt);
            vx *= d; vy *= d;
        }
        x += vx * dt;
        y += vy * dt;
        rot += vrot * dt;
    }

    public void render(GuiGraphicsExtractor g, Font font, int sw, int sh) {
        float t = clamp01(life / maxLife);
        float a = 1f - t;                 // linear fade-out
        switch (kind) {
            case GLYPH      -> renderGlyph(g, font, a);
            case SPARK      -> renderSpark(g, a);
            case FLAME      -> renderFlame(g, t, a, false);
            case SOUL       -> renderFlame(g, t, a, true);
            case SMOKE      -> renderSmoke(g, t, a);
            case DEBRIS     -> renderDebris(g, a);
            case SHOCKWAVE  -> renderShockwave(g, t, a);
            case FLASH      -> g.fill(0, 0, sw, sh, rgbAlpha(color & 0xFFFFFF, a * size));
            case CRIT       -> drawSparkle(g, x, y, size * (0.4f + t) * 4f, color, a);
            case ENCHANT    -> renderEnchant(g, a);
            case FIREWORK   -> renderFirework(g, a);
            case ITEM       -> renderItem(g);
        }
    }

    private void renderItem(GuiGraphicsExtractor g) {
        if (stack == null) return;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().rotate(rot);
        g.pose().scale(size);
        g.item(stack, -8, -8);   // g.item draws a 16x16 icon at the given top-left
        g.pose().popMatrix();
    }

    // ── per-kind drawing ─────────────────────────────────────────────────────────
    private void renderGlyph(GuiGraphicsExtractor g, Font font, float a) {
        if (glyph.isEmpty()) return;
        int w = font.width(glyph);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().rotate(rot);
        g.pose().scale(size);
        g.text(font, glyph, -w / 2, -4, withAlpha(color, a), true);
        g.pose().popMatrix();
    }

    private void renderSpark(GuiGraphicsExtractor g, float a) {
        int s = Math.max(1, Math.round(size));
        int ix = Math.round(x), iy = Math.round(y);
        g.fill(ix - s, iy - s, ix + s, iy + s, withAlpha(color, a));
        g.fill(ix, iy, ix + 1, iy + 1, rgbAlpha(0xFFFFFF, a)); // hot white core
    }

    /** Fire (orange/gold) or soul fire (cyan) — three nested teardrops that flicker and shrink as they rise. */
    private void renderFlame(GuiGraphicsExtractor g, float t, float a, boolean soul) {
        float flicker = 0.7f + 0.3f * (float) Math.sin(seed + life * 26f);
        float s = size * (1f - t) * flicker;
        int cx = Math.round(x), cy = Math.round(y);
        int outer = Math.max(1, Math.round(4 * s));
        int mid   = Math.max(1, Math.round(2.6f * s));
        int inner = Math.max(1, Math.round(1.3f * s));
        int cOuter = soul ? 0x3060CCFF : 0xCC2200;
        int cMid   = soul ? 0x40C8FF   : 0xFF7A10;
        int cInner = soul ? 0xCFFFFF   : 0xFFE070;
        g.fill(cx - outer, cy - outer - 1, cx + outer, cy + outer, rgbAlpha(cOuter, a * 0.6f));
        g.fill(cx - mid,   cy - mid - 2,   cx + mid,   cy + mid,   rgbAlpha(cMid,   a * 0.85f));
        g.fill(cx - inner, cy - inner - 2, cx + inner, cy + inner, rgbAlpha(cInner, a));
    }

    private void renderSmoke(GuiGraphicsExtractor g, float t, float a) {
        int r = Math.max(1, Math.round(size * (0.4f + t)));
        int cx = Math.round(x), cy = Math.round(y);
        g.fill(cx - r, cy - r, cx + r, cy + r, rgbAlpha(color & 0xFFFFFF, a * 0.22f));
    }

    private void renderDebris(GuiGraphicsExtractor g, float a) {
        int r = Math.max(1, Math.round(size));
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().rotate(rot);
        g.fill(-r, -r, r, r, withAlpha(color, a));
        g.pose().popMatrix();
    }

    private void renderShockwave(GuiGraphicsExtractor g, float t, float a) {
        int cx = Math.round(x), cy = Math.round(y);
        int rad = Math.round(size * t);
        int th = 2;
        int col = lerpColor(0xFFFFFFFF, color, t);
        col = withAlpha(col, a * 0.9f);
        ring(g, cx, cy, rad, th, col);
        if (rad > 6) ring(g, cx, cy, (int) (rad * 0.66f), 1, withAlpha(color, a * 0.5f));
    }

    private void renderEnchant(GuiGraphicsExtractor g, float a) {
        float tw = 0.5f + 0.5f * (float) Math.sin(seed + life * 18f);
        drawSparkle(g, x, y, size * 3.2f, color, a * tw);
    }

    private void renderFirework(GuiGraphicsExtractor g, float a) {
        int cx = Math.round(x), cy = Math.round(y);
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, withAlpha(color, a * 0.5f)); // glow
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, withAlpha(color, a));
        g.fill(cx, cy, cx + 1, cy + 1, rgbAlpha(0xFFFFFF, a));              // white core
    }

    // ── small drawing helpers ────────────────────────────────────────────────────
    private static void ring(GuiGraphicsExtractor g, int cx, int cy, int r, int th, int col) {
        if (r <= 0) return;
        g.fill(cx - r, cy - r, cx + r, cy - r + th, col);   // top
        g.fill(cx - r, cy + r - th, cx + r, cy + r, col);   // bottom
        g.fill(cx - r, cy - r, cx - r + th, cy + r, col);   // left
        g.fill(cx + r - th, cy - r, cx + r, cy + r, col);   // right
    }

    /** A 4-point sparkle/crit star: a bright plus with a hot core. No font glyph required. */
    private static void drawSparkle(GuiGraphicsExtractor g, float fx, float fy, float radius, int color, float a) {
        int cx = Math.round(fx), cy = Math.round(fy);
        int r = Math.max(1, Math.round(radius));
        int c = withAlpha(color, a);
        g.fill(cx, cy - r, cx + 1, cy + r, c);              // vertical
        g.fill(cx - r, cy, cx + r, cy + 1, c);              // horizontal
        int q = Math.max(1, r / 2);
        g.fill(cx - q, cy - q, cx + q, cy + q, withAlpha(color, a * 0.5f));
        g.fill(cx, cy, cx + 1, cy + 1, rgbAlpha(0xFFFFFF, a)); // core
    }

    // ── colour math ──────────────────────────────────────────────────────────────
    public static int withAlpha(int argb, float a) {
        int base = (argb >>> 24) & 0xFF;
        int al = Math.round(base * clamp01(a));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    public static int rgbAlpha(int rgb, float a) {
        int al = Math.round(255 * clamp01(a));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    public static int lerpColor(int x, int y, float t) {
        t = clamp01(t);
        int ax = (x >>> 24) & 0xFF, rx = (x >> 16) & 0xFF, gx = (x >> 8) & 0xFF, bx = x & 0xFF;
        int ay = (y >>> 24) & 0xFF, ry = (y >> 16) & 0xFF, gy = (y >> 8) & 0xFF, by = y & 0xFF;
        int ar = Math.round(ax + (ay - ax) * t);
        int rr = Math.round(rx + (ry - rx) * t);
        int gr = Math.round(gx + (gy - gx) * t);
        int br = Math.round(bx + (by - bx) * t);
        return (ar << 24) | (rr << 16) | (gr << 8) | br;
    }

    public static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
}
