package dev.limucc.brutaltyping.client.gui;

import dev.limucc.brutaltyping.client.compat.ScreenNav;
import dev.limucc.brutaltyping.client.compat.Gfx;
import dev.limucc.brutaltyping.client.config.BrutalConfig;
import dev.limucc.brutaltyping.client.config.BrutalConfigManager;
import dev.limucc.brutaltyping.client.gui.widget.FlatButton;
import dev.limucc.brutaltyping.client.gui.widget.SettingRow;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tabbed settings GUI in the Limucc flat style. Each setting <i>type</i> gets its own tab. A live "try it
 * here" box at the bottom fires the full effect on this very screen (via the EditBox + Screen mixins) so you
 * can feel your tuning instantly. All logic lives here; the per-MC-version {@code BrutalTypingScreen} subclass
 * only adapts the render + mouse-click entry points, which changed signature across versions.
 */
public abstract class BrutalScreenCore extends Screen {

    private static final String[] TAB_NAMES = {"General", "Voice", "Screen", "Brutality", "Amplifier", "Exclusive", "Info"};
    private static final String[] TAB_DESC = {
            "Master switches & where effects fire",
            "The sound your typing makes",
            "How hard the screen rattles",
            "Letters, fire, soul fire & explosions",
            "The dopamine amplifier",
            "Crits, weapons, impact frames & send-slam",
            "About the mod & its creator",
    };
    private static final int INFO_TAB = TAB_NAMES.length - 1;

    private static final String[] ABOUT = {
            "§l§6Brutal Typing  §r§7v1.0.0-beta.1",
            "",
            "Ridiculous-Coding for Minecraft — every keystroke detonates.",
            "Type faster → hotter amplifier → bigger booms & hotter letters.",
            "",
            "§eInspired by§r the \"Ridiculous Coding\" and \"Power Mode\"",
            "VS Code extensions.",
            "",
            "§eCreator§r    Limucc-dev",
            "§eGitHub§r     github.com/dev-limucc",
            "§eModrinth§r   modrinth.com/user/dev-limucc",
            "",
            "§8Client-side & for fun. Type in the box below to feel it.",
    };

    private final Screen parent;
    private int activeTab = 0;

    private int panelLeft, panelRight, panelW, listTop, listBottom;
    private final FlatButton[] tabs = new FlatButton[TAB_NAMES.length];
    private final FlatButton doneBtn = new FlatButton(0, 0, 0, 0, "Done");
    private final FlatButton resetBtn = new FlatButton(0, 0, 0, 0, "Reset Tab");

    private List<SettingRow> rows = new ArrayList<>();
    private int scroll = 0;
    private EditBox preview;

    protected BrutalScreenCore(Screen parent) {
        super(Component.literal("Brutal Typing"));
        this.parent = parent;
    }

    private static BrutalConfig cfg() { return BrutalConfigManager.get(); }
    private static void save() { BrutalConfigManager.save(); }

    @Override
    protected void init() {
        int cx = this.width / 2;
        panelLeft = cx - 220;
        panelRight = cx + 220;
        panelW = panelRight - panelLeft;

        int tabY = 24, tabW = panelW / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i] = new FlatButton(panelLeft + i * tabW, tabY, tabW - 2, 18, TAB_NAMES[i]);
        }

        listTop = 58;
        int footerY = this.height - 24;
        listBottom = footerY - 6;

        resetBtn.setBounds(panelLeft, footerY, 76, 18);
        doneBtn.setBounds(panelRight - 70, footerY, 70, 18);

        int pvX = panelLeft + 84;
        int pvW = (panelRight - 70) - pvX - 6;
        this.preview = new EditBox(this.font, pvX, footerY, pvW, 18, Component.literal("Preview"));
        this.preview.setHint(Component.literal("Try typing here…"));
        this.preview.setMaxLength(256);
        this.addRenderableWidget(this.preview);
        this.setInitialFocus(this.preview);

        buildRows();
    }

    // ── row construction per tab ─────────────────────────────────────────────────
    private void buildRows() {
        BrutalConfig c = cfg();
        List<SettingRow> r = new ArrayList<>();
        switch (activeTab) {
            case 0 -> { // General
                r.add(SettingRow.toggle("Master enable", () -> c.enabled, () -> { c.enabled = !c.enabled; save(); }));
                r.add(SettingRow.toggle("Effects in chat", () -> c.inChat, () -> { c.inChat = !c.inChat; save(); }));
                r.add(SettingRow.toggle("Effects in other text fields", () -> c.inTextFields, () -> { c.inTextFields = !c.inTextFields; save(); }));
                r.add(SettingRow.toggle("Effects on delete / backspace", () -> c.onDelete, () -> { c.onDelete = !c.onDelete; save(); }));
                r.add(SettingRow.toggle("Show combo / amplifier meter", () -> c.showComboMeter, () -> { c.showComboMeter = !c.showComboMeter; save(); }));
                r.add(SettingRow.cycle("Meter position", () -> c.meterPosition.display, () -> { c.meterPosition = c.meterPosition.next(); save(); }));
            }
            case 1 -> { // Voice
                r.add(SettingRow.cycle("Sound preset", () -> c.soundPreset.display, () -> { c.soundPreset = c.soundPreset.next(); save(); }));
                r.add(SettingRow.slider("Volume", 0.0, 1.5, 0.05, () -> c.soundVolume, v -> { c.soundVolume = (float) v; save(); }, BrutalScreenCore::f2));
                r.add(SettingRow.slider("Pitch (cold)", 0.5, 1.5, 0.05, () -> c.pitchMin, v -> { c.pitchMin = (float) v; save(); }, BrutalScreenCore::f2));
                r.add(SettingRow.slider("Pitch (hot)", 1.0, 2.0, 0.05, () -> c.pitchMax, v -> { c.pitchMax = (float) v; save(); }, BrutalScreenCore::f2));
                r.add(SettingRow.toggle("Milestone sounds", () -> c.milestoneSounds, () -> { c.milestoneSounds = !c.milestoneSounds; save(); }));
            }
            case 2 -> { // Screen
                r.add(SettingRow.cycle("Shake target", () -> c.shakeTarget.display, () -> { c.shakeTarget = c.shakeTarget.next(); save(); }));
                r.add(SettingRow.slider("Shake intensity", 0.0, 3.0, 0.1, () -> c.shakeIntensity, v -> { c.shakeIntensity = (float) v; save(); }, BrutalScreenCore::f1));
                r.add(SettingRow.slider("Settle speed", 4.0, 30.0, 1.0, () -> c.shakeDecay, v -> { c.shakeDecay = (float) v; save(); }, BrutalScreenCore::f1));
            }
            case 3 -> { // Brutality
                r.add(SettingRow.toggle("Brutal flying letters", () -> c.brutalLetters, () -> { c.brutalLetters = !c.brutalLetters; save(); }));
                r.add(SettingRow.toggle("Sparks", () -> c.sparks, () -> { c.sparks = !c.sparks; save(); }));
                r.add(SettingRow.toggle("Fire", () -> c.fire, () -> { c.fire = !c.fire; save(); }));
                r.add(SettingRow.toggle("Soul fire", () -> c.soulFire, () -> { c.soulFire = !c.soulFire; save(); }));
                r.add(SettingRow.toggle("TNT explosions", () -> c.explosions, () -> { c.explosions = !c.explosions; save(); }));
                r.add(SettingRow.toggle("Screen flash", () -> c.screenFlash, () -> { c.screenFlash = !c.screenFlash; save(); }));
                r.add(SettingRow.toggle("ALL-CAPS importance", () -> c.capsImportance, () -> { c.capsImportance = !c.capsImportance; save(); }));
                r.add(SettingRow.slider("Particle amount", 0.0, 2.0, 0.1, () -> c.particleAmount, v -> { c.particleAmount = (float) v; save(); }, BrutalScreenCore::f1));
                r.add(SettingRow.slider("Particle size", 0.2, 2.0, 0.1, () -> c.particleSize, v -> { c.particleSize = (float) v; save(); }, BrutalScreenCore::f1));
                r.add(SettingRow.slider("Gravity", 0.0, 2.0, 0.1, () -> c.gravity, v -> { c.gravity = (float) v; save(); }, BrutalScreenCore::f1));
            }
            case 4 -> { // Amplifier
                r.add(SettingRow.slider("Sensitivity", 0.03, 0.5, 0.01, () -> c.sensitivity, v -> { c.sensitivity = (float) v; save(); }, BrutalScreenCore::f2));
                r.add(SettingRow.slider("Cool-down", 0.2, 3.0, 0.05, () -> c.coolDown, v -> { c.coolDown = (float) v; save(); }, BrutalScreenCore::f2));
                r.add(SettingRow.slider("Max strength", 1.0, 5.0, 0.1, () -> c.strength, v -> { c.strength = (float) v; save(); }, BrutalScreenCore::f1));
                r.add(SettingRow.slider("Combo window", 400, 3000, 50, () -> c.comboWindowMs, v -> { c.comboWindowMs = (int) Math.round(v); save(); }, BrutalScreenCore::ms));
                r.add(SettingRow.toggle("WPM counter", () -> c.wpmCounter, () -> { c.wpmCounter = !c.wpmCounter; save(); }));
            }
            case 5 -> { // Exclusive
                r.add(SettingRow.toggle("Crit stars", () -> c.crit, () -> { c.crit = !c.crit; save(); }));
                r.add(SettingRow.toggle("Sharpness sparkles", () -> c.sharpness, () -> { c.sharpness = !c.sharpness; save(); }));
                r.add(SettingRow.toggle("Milestone fireworks", () -> c.fireworks, () -> { c.fireworks = !c.fireworks; save(); }));
                r.add(SettingRow.toggle("Weapon drops", () -> c.weaponDrops, () -> { c.weaponDrops = !c.weaponDrops; save(); }));
                r.add(SettingRow.slider("Weapon drop chance", 0.0, 0.5, 0.01, () -> c.weaponDropChance, v -> { c.weaponDropChance = (float) v; save(); }, BrutalScreenCore::pct));
                r.add(SettingRow.toggle("Impact frames (space)", () -> c.impactFrames, () -> { c.impactFrames = !c.impactFrames; save(); }));
                r.add(SettingRow.slider("Impact frame chance", 0.0, 0.5, 0.01, () -> c.impactFrameChance, v -> { c.impactFrameChance = (float) v; save(); }, BrutalScreenCore::pct));
                r.add(SettingRow.toggle("Send-slam animation", () -> c.sendSlam, () -> { c.sendSlam = !c.sendSlam; save(); }));
                r.add(SettingRow.toggle("Rainbow letters at max", () -> c.rainbowAtMax, () -> { c.rainbowAtMax = !c.rainbowAtMax; save(); }));
                r.add(SettingRow.slider("Milestone every (combo)", 5, 100, 1, () -> c.milestoneEvery, v -> { c.milestoneEvery = (int) Math.round(v); save(); }, BrutalScreenCore::intf));
            }
        }
        rows = r;
        scroll = 0;
    }

    private void resetTab() {
        BrutalConfig c = cfg();
        BrutalConfig d = new BrutalConfig();
        switch (activeTab) {
            case 0 -> { c.enabled = d.enabled; c.inChat = d.inChat; c.inTextFields = d.inTextFields; c.onDelete = d.onDelete; c.showComboMeter = d.showComboMeter; c.meterPosition = d.meterPosition; }
            case 1 -> { c.soundPreset = d.soundPreset; c.soundVolume = d.soundVolume; c.pitchMin = d.pitchMin; c.pitchMax = d.pitchMax; c.milestoneSounds = d.milestoneSounds; }
            case 2 -> { c.shakeTarget = d.shakeTarget; c.shakeIntensity = d.shakeIntensity; c.shakeDecay = d.shakeDecay; }
            case 3 -> { c.brutalLetters = d.brutalLetters; c.sparks = d.sparks; c.fire = d.fire; c.soulFire = d.soulFire; c.explosions = d.explosions; c.screenFlash = d.screenFlash; c.capsImportance = d.capsImportance; c.particleAmount = d.particleAmount; c.particleSize = d.particleSize; c.gravity = d.gravity; }
            case 4 -> { c.sensitivity = d.sensitivity; c.coolDown = d.coolDown; c.strength = d.strength; c.comboWindowMs = d.comboWindowMs; c.wpmCounter = d.wpmCounter; }
            case 5 -> { c.crit = d.crit; c.sharpness = d.sharpness; c.fireworks = d.fireworks; c.weaponDrops = d.weaponDrops; c.weaponDropChance = d.weaponDropChance; c.impactFrames = d.impactFrames; c.impactFrameChance = d.impactFrameChance; c.sendSlam = d.sendSlam; c.rainbowAtMax = d.rainbowAtMax; c.milestoneEvery = d.milestoneEvery; }
        }
        save();
    }

    private int maxScroll() {
        int total = rows.size() * SettingRow.ROW_H;
        int view = listBottom - listTop;
        return Math.max(0, total - view);
    }

    // ── rendering (called by the per-version subclass AFTER super.render) ────────
    protected void renderCore(Gfx g, int mouseX, int mouseY) {
        g.fill(panelLeft - 10, 4, panelRight + 10, this.height - 4, 0xC0121214);
        g.fill(panelLeft - 10, 4, panelRight + 10, 5, 0x22FFFFFF);

        String title = "§lBrutal Typing  §r§7— " + TAB_NAMES[activeTab];
        int tw = this.font.width(title);
        g.text(this.font, title, this.width / 2 - tw / 2, 10, 0xFFFFFFFF);

        for (int i = 0; i < tabs.length; i++) {
            tabs[i].render(g, this.font, mouseX, mouseY, true, i == activeTab);
        }
        g.text(this.font, "§7" + TAB_DESC[activeTab], panelLeft, 45, 0xFF909090);

        if (activeTab == INFO_TAB) {
            int y = listTop + 4;
            for (String line : ABOUT) {
                g.text(this.font, line, panelLeft, y, 0xFFD8D8D8);
                y += 12;
            }
        } else {
            // content list (scissored)
            g.fill(panelLeft - 2, listTop - 2, panelRight + 2, listBottom + 2, 0x40000000);
            g.enableScissor(panelLeft - 2, listTop, panelRight + 2, listBottom);
            for (int i = 0; i < rows.size(); i++) {
                SettingRow row = rows.get(i);
                row.setBounds(panelLeft, listTop + i * SettingRow.ROW_H - scroll, panelW);
                row.render(g, this.font, mouseX, mouseY);
            }
            g.disableScissor();

            // scrollbar
            int total = rows.size() * SettingRow.ROW_H, view = listBottom - listTop;
            if (total > view) {
                int barH = Math.max(15, view * view / total);
                int barY = listTop + scroll * (view - barH) / (total - view);
                g.fill(panelRight + 2, barY, panelRight + 5, barY + barH, 0xFFAAAAAA);
            }
        }

        g.text(this.font, "§8type ↓", panelLeft, this.height - 22, 0xFF707070);
        resetBtn.render(g, this.font, mouseX, mouseY, true);
        doneBtn.render(g, this.font, mouseX, mouseY, true);
    }

    // ── input (called by the per-version subclass for LEFT clicks) ───────────────
    protected boolean clickCore(double mx, double my) {
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i].contains(mx, my)) {
                if (i != activeTab) { activeTab = i; buildRows(); }
                return true;
            }
        }
        if (doneBtn.contains(mx, my)) { this.onClose(); return true; }
        if (resetBtn.contains(mx, my)) { resetTab(); buildRows(); return true; }

        if (my >= listTop && my < listBottom) {
            for (SettingRow row : rows) {
                if (row.mouseClicked(mx, my)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (my >= listTop && my < listBottom) {
            for (SettingRow row : rows) {
                if (row.inRow(my)) {
                    if (row.mouseScrolled(mx, my, scrollY)) return true;
                    break;
                }
            }
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (scrollY * SettingRow.ROW_H)));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        save();
        ScreenNav.open(this.minecraft, this.parent);
    }

    // ── value formatters (Locale.ROOT so decimals stay dots) ─────────────────────
    private static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }
    private static String f2(double v) { return String.format(Locale.ROOT, "%.2f", v); }
    private static String ms(double v) { return Math.round(v) + "ms"; }
    private static String intf(double v) { return Long.toString(Math.round(v)); }
    private static String pct(double v) { return Math.round(v * 100) + "%"; }
}
