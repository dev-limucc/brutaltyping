package dev.limucc.brutaltyping.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * One settings row in the Limucc flat style: a left-aligned label plus a right-aligned control. Handles
 * toggles, enum cyclers and sliders. To avoid relying on mouseDragged, sliders are click-to-set and
 * scroll-to-nudge; cyclers also advance on scroll.
 */
public class SettingRow {

    public enum Type { TOGGLE, CYCLE, SLIDER }

    public static final int ROW_H = 24;
    private static final int CONTROL_W = 132;
    private static final int CONTROL_H = 16;

    public final Type type;
    public final String label;

    // toggle
    private BooleanSupplier getBool;
    private Runnable onToggle;
    // cycle
    private Supplier<String> valueText;
    private Runnable onCycle;
    // slider
    private double min, max, step;
    private DoubleSupplier getVal;
    private DoubleConsumer setVal;
    private Function<Double, String> fmt;

    public int x, y, w;

    private SettingRow(Type type, String label) { this.type = type; this.label = label; }

    public static SettingRow toggle(String label, BooleanSupplier get, Runnable onToggle) {
        SettingRow r = new SettingRow(Type.TOGGLE, label);
        r.getBool = get; r.onToggle = onToggle;
        return r;
    }

    public static SettingRow cycle(String label, Supplier<String> valueText, Runnable onCycle) {
        SettingRow r = new SettingRow(Type.CYCLE, label);
        r.valueText = valueText; r.onCycle = onCycle;
        return r;
    }

    public static SettingRow slider(String label, double min, double max, double step,
                                    DoubleSupplier get, DoubleConsumer set, Function<Double, String> fmt) {
        SettingRow r = new SettingRow(Type.SLIDER, label);
        r.min = min; r.max = max; r.step = step; r.getVal = get; r.setVal = set; r.fmt = fmt;
        return r;
    }

    public void setBounds(int x, int y, int w) { this.x = x; this.y = y; this.w = w; }

    private int controlX() { return x + w - CONTROL_W; }
    private int controlY() { return y + (ROW_H - CONTROL_H) / 2; }

    private boolean inControl(double mx, double my) {
        return mx >= controlX() && mx < controlX() + CONTROL_W && my >= controlY() && my < controlY() + CONTROL_H;
    }

    public boolean inRow(double my) { return my >= y && my < y + ROW_H; }

    public void render(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        g.text(font, label, x, y + (ROW_H - 8) / 2, 0xFFE0E0E0);
        int cx = controlX(), cy = controlY();
        boolean hover = inControl(mouseX, mouseY);

        switch (type) {
            case TOGGLE -> {
                boolean on = getBool.getAsBoolean();
                box(g, cx, cy, hover);
                String s = on ? "§aON" : "§7OFF";
                int tw = font.width(s);
                g.text(font, s, cx + (CONTROL_W - tw) / 2, cy + (CONTROL_H - 8) / 2, 0xFFFFFFFF);
            }
            case CYCLE -> {
                box(g, cx, cy, hover);
                String s = valueText.get();
                int tw = font.width(s);
                g.text(font, s, cx + (CONTROL_W - tw) / 2, cy + (CONTROL_H - 8) / 2, 0xFFFFFF80);
            }
            case SLIDER -> {
                double frac = (getVal.getAsDouble() - min) / (max - min);
                frac = Math.max(0, Math.min(1, frac));
                int fillW = (int) Math.round(frac * CONTROL_W);
                g.fill(cx, cy, cx + CONTROL_W, cy + CONTROL_H, 0xFF1C1C20);
                g.fill(cx, cy, cx + fillW, cy + CONTROL_H, hover ? 0xFF3A6EA5 : 0xFF2E5A86);
                g.fill(cx, cy, cx + CONTROL_W, cy + 1, 0x22FFFFFF);
                g.fill(cx, cy + CONTROL_H - 1, cx + CONTROL_W, cy + CONTROL_H, 0x44000000);
                int handleX = Math.min(cx + CONTROL_W - 2, cx + fillW);
                g.fill(handleX - 1, cy - 1, handleX + 2, cy + CONTROL_H + 1, 0xFFFFFFFF);
                String s = fmt.apply(getVal.getAsDouble());
                int tw = font.width(s);
                g.text(font, s, cx + (CONTROL_W - tw) / 2, cy + (CONTROL_H - 8) / 2, 0xFFFFFF80);
            }
        }
    }

    private static void box(GuiGraphicsExtractor g, int cx, int cy, boolean hover) {
        g.fill(cx, cy, cx + CONTROL_W, cy + CONTROL_H, hover ? 0xFF3A6EA5 : 0xFF26262B);
        g.fill(cx, cy, cx + CONTROL_W, cy + 1, 0x22FFFFFF);
        g.fill(cx, cy + CONTROL_H - 1, cx + CONTROL_W, cy + CONTROL_H, 0x44000000);
    }

    /** Left-click. Returns true if it consumed the click. */
    public boolean mouseClicked(double mx, double my) {
        if (!inRow(my)) return false;
        switch (type) {
            case TOGGLE -> { if (inControl(mx, my)) { onToggle.run(); return true; } }
            case CYCLE -> { if (inControl(mx, my)) { onCycle.run(); return true; } }
            case SLIDER -> {
                if (inControl(mx, my)) {
                    double frac = (mx - controlX()) / (double) CONTROL_W;
                    setSnapped(min + Math.max(0, Math.min(1, frac)) * (max - min));
                    return true;
                }
            }
        }
        return false;
    }

    /** Scroll wheel over the row. dir > 0 = up. */
    public boolean mouseScrolled(double mx, double my, double dir) {
        if (!inRow(my)) return false;
        switch (type) {
            case SLIDER -> { setSnapped(getVal.getAsDouble() + Math.signum(dir) * step); return true; }
            case CYCLE  -> { onCycle.run(); return true; }
            default     -> { return false; }
        }
    }

    private void setSnapped(double v) {
        v = Math.max(min, Math.min(max, v));
        double snapped = Math.round(v / step) * step;
        snapped = Math.max(min, Math.min(max, snapped));
        setVal.accept(snapped);
    }
}
