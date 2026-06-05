package dev.limucc.brutaltyping.client.engine;

import dev.limucc.brutaltyping.client.config.BrutalConfig;
import dev.limucc.brutaltyping.client.config.BrutalConfigManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The dopamine engine. Every keystroke heats up an amplifier; the faster you type, the higher {@code heat}
 * (0..1) climbs, and every effect scales off it. Also handles ALL-CAPS "importance", spacebar impact frames,
 * falling weapon textures, and the weapon-slam-on-send animation. One global instance drives the EditBox mixin
 * (input), the Screen mixin and the Gui/HUD mixin (render + shake).
 */
public final class EffectEngine {

    public static final EffectEngine INSTANCE = new EffectEngine();

    private static final int MAX_PARTICLES = 1500;

    private final List<Particle> particles = new ArrayList<>();
    private final SoundDirector sound = new SoundDirector();
    private final Random rand = new Random();

    // amplifier state
    private float heat = 0f;
    private int combo = 0;
    private long lastTypeNano = 0L;

    // WPM ring buffer (intervals in ms between keystrokes)
    private final float[] intervals = new float[12];
    private int intervalIdx = 0, intervalCount = 0;

    // screen shake
    private float shakeMag = 0f, shakeX = 0f, shakeY = 0f;

    // comic impact frame
    private float impactLife = 0f;
    private static final float IMPACT_MAX = 0.24f;

    // send slam
    private boolean slamActive = false, slamImpacted = false;
    private float slamLife = 0f, slamIntensity = 0f;
    private static final float SLAM_MAX = 0.6f;
    private ItemStack slamWeapon;

    // frame timing
    private long lastRenderNano = 0L;
    private float dt = 0f;

    private EffectEngine() {}

    private static BrutalConfig cfg() { return BrutalConfigManager.get(); }

    public float heat() { return heat; }
    public int combo() { return combo; }
    public float shakeX() { return shakeX; }
    public float shakeY() { return shakeY; }

    // ── input events (from EditBoxMixin) ─────────────────────────────────────────
    public void onChar(int codepoint, float caretX, float caretY, boolean important) {
        BrutalConfig c = cfg();
        if (!c.enabled) return;

        long now = System.nanoTime();
        float interval = (lastTypeNano == 0L) ? 9999f : (now - lastTypeNano) / 1_000_000f;
        lastTypeNano = now;

        combo = (interval <= c.comboWindowMs) ? combo + 1 : 1;

        float sf;
        if (interval <= 90f) sf = 1f;
        else if (interval >= 600f) sf = 0.12f;
        else sf = 1f - (interval - 90f) / (600f - 90f) * (1f - 0.12f);
        heat = Particle.clamp01(heat + c.sensitivity * (0.25f + 1.4f * sf));

        pushInterval(interval);
        spawnKeystroke(codepoint, caretX, caretY, c, important);
        sound.keystroke(heat, c);
        addShake(c.shakeIntensity * (1.2f + heat * 6f) * (important ? 1.6f : 1f));

        if (codepoint == ' ' && c.impactFrames && rand.nextFloat() < c.impactFrameChance) {
            impactLife = IMPACT_MAX;
            addShake(c.shakeIntensity * 7f);
            sound.impact(c);
        }
        if (c.weaponDrops && rand.nextFloat() < c.weaponDropChance) {
            spawnWeapon(caretX, caretY, c);
        }
        if (c.milestoneEvery > 0 && combo > 0 && combo % c.milestoneEvery == 0) {
            milestone(caretX, caretY, c);
        }
    }

    public void onDelete(float caretX, float caretY) {
        BrutalConfig c = cfg();
        if (!c.enabled || !c.onDelete) return;
        sound.delete(c);   // a different demolition sound each time
        addShake(c.shakeIntensity * 4f);
        heat = Particle.clamp01(heat - 0.04f);
        if (c.explosions) {
            Particle sw = base(Particle.Kind.SHOCKWAVE, caretX, caretY);
            sw.size = 14f; sw.maxLife = 0.30f; sw.color = 0xFFFF6060;
            add(sw);
        }
        int debris = Math.round(4 * c.particleAmount);
        for (int i = 0; i < debris; i++) spawnDebris(caretX, caretY, c, 0xFF884422);
    }

    /** Hit-send: a weapon swings up and smashes your message into the chat. Intensity = how hard you pushed. */
    public void sendSlam(String message) {
        BrutalConfig c = cfg();
        if (!c.enabled || !c.sendSlam) return;
        slamActive = true;
        slamImpacted = false;
        slamLife = 0f;
        int len = message == null ? 0 : Math.min(message.length(), 50);
        slamIntensity = Particle.clamp01(0.3f + heat * 0.6f + len / 50f * 0.4f);
        slamWeapon = new ItemStack(Items.MACE);
        sound.slamCharge(c);
    }

    // ── per-client-tick decay (from BrutalTypingClient) ──────────────────────────
    public void tick() {
        BrutalConfig c = cfg();
        heat = Math.max(0f, heat - c.coolDown / 20f);
        if (combo > 0 && lastTypeNano != 0L
                && (System.nanoTime() - lastTypeNano) / 1_000_000f > c.comboWindowMs) {
            combo = 0;
        }
    }

    // ── frame hooks (from ScreenMixin HEAD / GuiMixin) ───────────────────────────
    public void beginFrame() {
        long now = System.nanoTime();
        dt = (lastRenderNano == 0L) ? 1f / 60f : (now - lastRenderNano) / 1_000_000_000f;
        if (dt < 0f) dt = 0f;
        if (dt > 0.1f) dt = 0.1f;
        lastRenderNano = now;

        float decay = (float) Math.exp(-cfg().shakeDecay * dt);
        shakeMag *= decay;
        if (shakeMag < 0.05f) shakeMag = 0f;
        shakeX = (rand.nextFloat() * 2f - 1f) * shakeMag;
        shakeY = (rand.nextFloat() * 2f - 1f) * shakeMag;

        if (impactLife > 0f) impactLife = Math.max(0f, impactLife - dt);
    }

    /** Draw particles, the slam, the impact frame, then the (optional) meter. Used by both screen and HUD. */
    public void renderOverlay(GuiGraphicsExtractor g, int sw, int sh, Font font) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update(dt);
            if (p.dead()) particles.remove(i);
            else p.render(g, font, sw, sh);
        }
        renderSlam(g, sw, sh);
        renderImpact(g, sw, sh);
        renderMeter(g, sw, sh, font);
    }

    // ── spawning ─────────────────────────────────────────────────────────────────
    private void spawnKeystroke(int codepoint, float x, float y, BrutalConfig c, boolean important) {
        boolean caps = important && c.capsImportance;
        float amount = c.particleAmount;
        float strength = c.strength;
        float sizeBoost = caps ? 1.7f : 1f;

        if (c.brutalLetters) {
            String glyph = glyphFor(codepoint);
            if (!glyph.isEmpty()) {
                int bits = 1 + (heat > 0.6f ? 1 : 0) + (heat > 0.9f ? 1 : 0) + (caps ? 1 : 0);
                int color = caps ? Particle.lerpColor(letterColor(heat, c), 0xFFFF2A00, 0.65f) : letterColor(heat, c);
                String text = caps ? "§l" + glyph : glyph;     // §l bold for importance
                for (int i = 0; i < bits; i++) {
                    Particle p = base(Particle.Kind.GLYPH, x, y);
                    p.glyph = text;
                    p.vx = (rand.nextFloat() * 2f - 1f) * (20f + heat * 60f);
                    p.vy = -(20f + heat * 70f) - rand.nextFloat() * 20f;
                    p.ay = 120f * c.gravity;
                    p.vrot = (rand.nextFloat() * 2f - 1f) * (4f + heat * 10f);
                    p.size = (1f + heat * 1.6f) * c.particleSize * sizeBoost;
                    p.maxLife = 0.55f + heat * 0.5f;
                    p.color = color;
                    p.drag = 1.2f;
                    add(p);
                }
            }
        }

        if (c.sparks) {
            int n = Math.round((2f + heat * (4f + strength * 3f)) * amount);
            for (int i = 0; i < n; i++) spawnSpark(x, y, c);
        }
        if (c.fire && (heat > 0.25f || caps)) {
            int n = Math.round((1f + heat * 4f + (caps ? 3f : 0f)) * amount);
            for (int i = 0; i < n; i++) spawnFlame(x, y, c, false);
        }
        if (c.soulFire && heat > 0.6f) {
            int n = Math.round((1f + heat * 3f) * amount);
            for (int i = 0; i < n; i++) spawnFlame(x, y, c, true);
        }
        if (caps) {                                   // an importance "pop"
            Particle sw = base(Particle.Kind.SHOCKWAVE, x, y);
            sw.size = 16f + heat * 16f; sw.maxLife = 0.28f; sw.color = 0xFFFF5520;
            add(sw);
        }
        if (c.crit && heat > 0.72f && rand.nextFloat() < 0.5f) {
            spawnSparkleParticle(Particle.Kind.CRIT, x, y, c, 0xFFFFFFFF);
        }
        if (c.sharpness && heat > 0.85f && rand.nextFloat() < 0.45f) {
            spawnSparkleParticle(Particle.Kind.ENCHANT, x, y, c, 0xFFB452FF);
        }
    }

    private void milestone(float x, float y, BrutalConfig c) {
        sound.milestone(c);
        addShake(c.shakeIntensity * 8f);
        explosionBurst(x, y, 0.6f + heat, c);
        if (c.fireworks) {
            int stars = Math.round(18 * c.particleAmount);
            int baseColor = hsv(rand.nextFloat());
            for (int i = 0; i < stars; i++) {
                Particle p = base(Particle.Kind.FIREWORK, x, y);
                double ang = rand.nextDouble() * Math.PI * 2;
                float spd = 60f + rand.nextFloat() * 90f;
                p.vx = (float) Math.cos(ang) * spd;
                p.vy = (float) Math.sin(ang) * spd;
                p.ay = 60f * c.gravity; p.drag = 1.8f;
                p.maxLife = 0.6f + rand.nextFloat() * 0.5f;
                p.color = (rand.nextFloat() < 0.5f) ? baseColor : hsv(rand.nextFloat());
                add(p);
            }
        }
    }

    /** Shared TNT-style blast (flash + shockwave + smoke + debris), scaled by {@code power}. */
    private void explosionBurst(float x, float y, float power, BrutalConfig c) {
        if (!c.explosions) return;
        Particle sw = base(Particle.Kind.SHOCKWAVE, x, y);
        sw.size = 28f + power * 40f; sw.maxLife = 0.42f; sw.color = 0xFFFFAA30;
        add(sw);
        if (c.screenFlash) {
            Particle fl = base(Particle.Kind.FLASH, x, y);
            fl.color = 0xFFFFFFFF; fl.size = 0.16f + power * 0.16f; fl.maxLife = 0.13f;
            add(fl);
        }
        int smoke = Math.round(5 * c.particleAmount);
        for (int i = 0; i < smoke; i++) {
            Particle p = base(Particle.Kind.SMOKE, x, y);
            p.vx = (rand.nextFloat() * 2f - 1f) * 30f;
            p.vy = -25f - rand.nextFloat() * 30f;
            p.size = 4f + rand.nextFloat() * 5f;
            p.maxLife = 0.7f + rand.nextFloat() * 0.5f;
            p.color = 0xFF606060; p.drag = 1.5f;
            add(p);
        }
        int debris = Math.round(8 * c.particleAmount * Math.max(0.6f, power));
        for (int i = 0; i < debris; i++) spawnDebris(x, y, c, 0xFF552200);
    }

    private void spawnWeapon(float x, float y, BrutalConfig c) {
        Item it = switch (rand.nextInt(9)) {
            case 0 -> Items.NETHERITE_SWORD;
            case 1 -> Items.DIAMOND_AXE;
            case 2 -> Items.MACE;
            case 3 -> Items.TRIDENT;
            case 4 -> Items.DIAMOND_SWORD;
            case 5 -> Items.IRON_SWORD;
            case 6 -> Items.NETHERITE_AXE;
            case 7 -> Items.BOW;
            default -> Items.CROSSBOW;
        };
        Particle p = base(Particle.Kind.ITEM, x + (rand.nextFloat() * 2f - 1f) * 60f, y - 90f - rand.nextFloat() * 40f);
        p.stack = new ItemStack(it);
        p.vy = 25f + rand.nextFloat() * 40f;
        p.ay = 220f * Math.max(0.4f, c.gravity);
        p.vrot = (rand.nextFloat() * 2f - 1f) * 6f;
        p.size = (1.3f + rand.nextFloat() * 0.5f) * c.particleSize;
        p.maxLife = 1.7f;
        add(p);
        sound.weapon(weaponSound(it), c);
    }

    private SoundEvent weaponSound(Item it) {
        if (it == Items.MACE) return SoundEvents.MACE_SMASH_AIR;
        if (it == Items.TRIDENT) return SoundEvents.TRIDENT_THROW.value();
        if (it == Items.BOW || it == Items.CROSSBOW) return SoundEvents.PLAYER_ATTACK_WEAK;
        return switch (rand.nextInt(3)) {
            case 0 -> SoundEvents.PLAYER_ATTACK_STRONG;
            case 1 -> SoundEvents.PLAYER_ATTACK_SWEEP;
            default -> SoundEvents.PLAYER_ATTACK_KNOCKBACK;
        };
    }

    private void spawnSpark(float x, float y, BrutalConfig c) {
        Particle p = base(Particle.Kind.SPARK, x, y);
        double ang = rand.nextDouble() * Math.PI * 2;
        float spd = (25f + heat * 110f) * (0.4f + rand.nextFloat());
        p.vx = (float) Math.cos(ang) * spd;
        p.vy = (float) Math.sin(ang) * spd - 15f;
        p.ay = 90f * c.gravity; p.drag = 2.2f;
        p.size = (1f + heat * 1.6f) * c.particleSize;
        p.maxLife = 0.3f + rand.nextFloat() * 0.4f;
        p.color = letterColor(heat, c);
        add(p);
    }

    private void spawnFlame(float x, float y, BrutalConfig c, boolean soul) {
        Particle p = base(soul ? Particle.Kind.SOUL : Particle.Kind.FLAME, x, y);
        p.x += (rand.nextFloat() * 2f - 1f) * 6f;
        p.vx = (rand.nextFloat() * 2f - 1f) * 12f;
        p.vy = -(25f + heat * 45f) - rand.nextFloat() * 15f;
        p.size = (1.2f + heat * 1.5f) * c.particleSize;
        p.maxLife = 0.35f + rand.nextFloat() * 0.4f;
        p.seed = rand.nextFloat() * 6.28f;
        add(p);
    }

    private void spawnDebris(float x, float y, BrutalConfig c, int color) {
        Particle p = base(Particle.Kind.DEBRIS, x, y);
        double ang = rand.nextDouble() * Math.PI * 2;
        float spd = 40f + rand.nextFloat() * 90f;
        p.vx = (float) Math.cos(ang) * spd;
        p.vy = (float) Math.sin(ang) * spd - 40f;
        p.ay = 200f * c.gravity; p.drag = 0.6f;
        p.size = (1f + rand.nextFloat() * 1.5f) * c.particleSize;
        p.vrot = (rand.nextFloat() * 2f - 1f) * 12f;
        p.maxLife = 0.5f + rand.nextFloat() * 0.5f;
        p.color = color;
        add(p);
    }

    private void spawnSparkleParticle(Particle.Kind kind, float x, float y, BrutalConfig c, int color) {
        int n = 1 + rand.nextInt(2);
        for (int i = 0; i < n; i++) {
            Particle p = base(kind, x, y);
            p.x += (rand.nextFloat() * 2f - 1f) * 14f;
            p.y += (rand.nextFloat() * 2f - 1f) * 10f;
            p.vy = -10f - rand.nextFloat() * 14f;
            p.size = (0.8f + rand.nextFloat() * 0.6f) * c.particleSize;
            p.maxLife = 0.45f + rand.nextFloat() * 0.35f;
            p.seed = rand.nextFloat() * 6.28f;
            p.color = color;
            add(p);
        }
    }

    private Particle base(Particle.Kind kind, float x, float y) {
        Particle p = new Particle(kind);
        p.x = x; p.y = y;
        return p;
    }

    private void add(Particle p) {
        if (particles.size() >= MAX_PARTICLES) return;
        particles.add(p);
    }

    private void addShake(float px) {
        shakeMag = Math.min(16f, Math.max(shakeMag, px));
    }

    // ── send slam render ─────────────────────────────────────────────────────────
    private void renderSlam(GuiGraphicsExtractor g, int sw, int sh) {
        if (!slamActive) return;
        slamLife += dt;
        float t = Particle.clamp01(slamLife / SLAM_MAX);
        float impactY = sh - 46f;
        float wx = sw / 2f;
        float riseT = Math.min(1f, t / 0.5f);
        float ease = 1f - (1f - riseT) * (1f - riseT);
        float weaponY = (sh + 40f) + (impactY - (sh + 40f)) * ease;

        if (!slamImpacted && t >= 0.5f) {
            slamImpacted = true;
            explosionBurst(wx, impactY, 0.7f + slamIntensity, cfg());
            sound.slamHit(slamIntensity, cfg());
            addShake(cfg().shakeIntensity * (8f + slamIntensity * 8f));
        }
        float drawY = slamImpacted ? impactY - (t - 0.5f) * 160f : weaponY;
        float scale = 1.6f + slamIntensity * 2.6f;
        float rot = (1f - t) * 1.8f - 0.4f;
        if (slamWeapon != null) {
            g.pose().pushMatrix();
            g.pose().translate(wx, drawY);
            g.pose().rotate(rot);
            g.pose().scale(scale);
            g.item(slamWeapon, -8, -8);
            g.pose().popMatrix();
        }
        if (slamLife >= SLAM_MAX) { slamActive = false; slamImpacted = false; slamWeapon = null; }
    }

    // ── comic impact frame render ────────────────────────────────────────────────
    private void renderImpact(GuiGraphicsExtractor g, int sw, int sh) {
        if (impactLife <= 0f) return;
        float p = impactLife / IMPACT_MAX;                  // 1 -> 0
        g.fill(0, 0, sw, sh, Particle.rgbAlpha(0xFFFFFF, Math.min(1f, p) * 0.5f));
        int bar = Math.round(sh * 0.13f * (float) Math.sin(Math.PI * (1f - p)));
        if (bar > 0) {
            g.fill(0, 0, sw, bar, 0xDD000000);
            g.fill(0, sh - bar, sw, sh, 0xDD000000);
        }
        int cx = sw / 2, cy = sh / 2;
        float inner = (1f - p) * Math.max(sw, sh) * 0.5f;
        float outer = Math.max(sw, sh);
        int col = Particle.rgbAlpha(0x101010, p * 0.6f);
        int lines = 28;
        for (int i = 0; i < lines; i++) {
            float ang = (float) (i * Math.PI * 2 / lines) + p;
            int th = 2 + (i % 3);
            g.pose().pushMatrix();
            g.pose().translate(cx, cy);
            g.pose().rotate(ang);
            g.fill((int) inner, -th, (int) outer, th, col);
            g.pose().popMatrix();
        }
    }

    // ── meter (compact, repositionable, hidden by default) ───────────────────────
    private void renderMeter(GuiGraphicsExtractor g, int sw, int sh, Font font) {
        BrutalConfig c = cfg();
        if (!c.showComboMeter) return;
        if (heat < 0.02f && combo <= 1) return;

        Tier tier = Tier.of(heat);
        int w = 100;
        int x, y;
        switch (c.meterPosition) {
            case TOP_LEFT    -> { x = 4;          y = 4; }
            case TOP_RIGHT   -> { x = sw - w - 6; y = 4; }
            case BOTTOM_LEFT -> { x = 4;          y = sh - 24; }
            default          -> { x = 4;          y = sh - 40; } // ABOVE_CHAT
        }

        StringBuilder sb = new StringBuilder("§l").append(tier.label);
        if (combo > 1) sb.append("§r x").append(combo);
        if (c.wpmCounter) { int wpm = wpm(); if (wpm > 0) sb.append(" §7").append(wpm); }
        int labelColor = (c.rainbowAtMax && tier == Tier.APOCALYPSE)
                ? hsv((System.nanoTime() / 4_000_000L % 360) / 360f) : tier.color;
        g.text(font, sb.toString(), x, y, labelColor);

        int by = y + 10;
        g.fill(x, by, x + w, by + 3, 0xC0000000);
        int fillW = Math.round(w * Particle.clamp01(heat));
        int dim = Particle.lerpColor(0xFF000000 | (tier.color & 0xFFFFFF), tier.color, 0.35f);
        g.fillGradient(x, by, x + fillW, by + 3, dim, tier.color);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────
    private void pushInterval(float ms) {
        if (ms > 3000f) return;
        intervals[intervalIdx] = ms;
        intervalIdx = (intervalIdx + 1) % intervals.length;
        if (intervalCount < intervals.length) intervalCount++;
    }

    private int wpm() {
        if (intervalCount == 0) return 0;
        float sum = 0f; int n = 0;
        for (int i = 0; i < intervalCount; i++) {
            float v = intervals[i];
            if (v > 0f && v < 2000f) { sum += v; n++; }
        }
        if (n == 0) return 0;
        float avg = sum / n;
        return Math.max(0, Math.min(999, Math.round(60000f / (avg * 5f))));
    }

    private String glyphFor(int cp) {
        if (cp <= 0x20 || cp == 0x7F) return "";
        if (!Character.isValidCodePoint(cp)) return "";
        return new String(Character.toChars(cp));
    }

    private int letterColor(float h, BrutalConfig c) {
        if (c.rainbowAtMax && h > 0.92f) return hsv(rand.nextFloat());
        float[] stops = {0f, 0.3f, 0.5f, 0.7f, 0.9f, 1f};
        int[] cols = {0xFFFFFFFF, 0xFFFFE060, 0xFFFFA020, 0xFFFF5512, 0xFFFF2010, 0xFFFFFFFF};
        for (int i = 0; i < stops.length - 1; i++) {
            if (h <= stops[i + 1]) {
                float t = (h - stops[i]) / (stops[i + 1] - stops[i]);
                return Particle.lerpColor(cols[i], cols[i + 1], t);
            }
        }
        return cols[cols.length - 1];
    }

    private static int hsv(float h) {
        h = (h % 1f + 1f) % 1f;
        float r = 0, g = 0, b = 0;
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float q = 1f - f, tt = f;
        switch (i % 6) {
            case 0 -> { r = 1; g = tt; b = 0; }
            case 1 -> { r = q; g = 1; b = 0; }
            case 2 -> { r = 0; g = 1; b = tt; }
            case 3 -> { r = 0; g = q; b = 1; }
            case 4 -> { r = tt; g = 0; b = 1; }
            case 5 -> { r = 1; g = 0; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
