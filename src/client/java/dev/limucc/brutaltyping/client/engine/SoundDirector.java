package dev.limucc.brutaltyping.client.engine;

import dev.limucc.brutaltyping.client.config.BrutalConfig;
import dev.limucc.brutaltyping.client.config.SoundPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

/**
 * Turns typing into noise. Each keystroke fires the chosen {@link SoundPreset} with a pitch that rises as the
 * amplifier heats up (Power-Mode style). Sounds are throttled so fast mashing doesn't become an audio wall.
 */
public final class SoundDirector {

    private final Random rand = new Random();
    private long lastSoundNano = 0L;
    private static final long MIN_GAP_NANOS = 30_000_000L; // ~30ms between per-key sounds

    public void keystroke(float heat, BrutalConfig cfg) {
        if (cfg.soundPreset.muted() || cfg.soundVolume <= 0.01f) return;
        long now = System.nanoTime();
        if (now - lastSoundNano < MIN_GAP_NANOS) return;
        lastSoundNano = now;

        float pitch = cfg.pitchMin + (cfg.pitchMax - cfg.pitchMin) * heat;
        float volume = cfg.soundVolume * (0.45f + 0.55f * heat);
        play(cfg.soundPreset.primary(), pitch, volume);
    }

    public void milestone(BrutalConfig cfg) {
        if (!cfg.milestoneSounds || cfg.soundPreset.muted()) return;
        play(cfg.soundPreset.milestone(), 1.0f, Math.max(0.6f, cfg.soundVolume));
    }

    /** Deletions feel destructive — and pick a different demolition sound each time. */
    public void delete(BrutalConfig cfg) {
        if (cfg.soundVolume <= 0.01f) return;
        SoundEvent ev = switch (rand.nextInt(8)) {
            case 0 -> SoundEvents.ANVIL_LAND;
            case 1 -> SoundEvents.ANVIL_DESTROY;
            case 2 -> SoundEvents.GLASS_BREAK;
            case 3 -> SoundEvents.WOOD_BREAK;
            case 4 -> SoundEvents.STONE_BREAK;
            case 5 -> SoundEvents.GRAVEL_BREAK;
            case 6 -> SoundEvents.ITEM_BREAK.value();
            default -> SoundEvents.NETHERITE_BLOCK_BREAK;
        };
        play(ev, 0.6f + rand.nextFloat() * 0.6f, Math.min(1.0f, cfg.soundVolume));
    }

    /** Quick whoosh when a comic impact frame fires. */
    public void impact(BrutalConfig cfg) {
        if (cfg.soundVolume <= 0.01f) return;
        play(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.6f * cfg.soundVolume);
    }

    /** A weapon's own sound when it drops in. */
    public void weapon(SoundEvent ev, BrutalConfig cfg) {
        if (ev == null || cfg.soundVolume <= 0.01f) return;
        play(ev, 0.9f + rand.nextFloat() * 0.3f, 0.6f * cfg.soundVolume);
    }

    /** The rising swing when you hit send. */
    public void slamCharge(BrutalConfig cfg) {
        if (cfg.soundVolume <= 0.01f) return;
        play(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.7f * cfg.soundVolume);
    }

    /** The mace landing on your message — scaled by how hard you pushed it out. */
    public void slamHit(float intensity, BrutalConfig cfg) {
        if (cfg.soundVolume <= 0.01f) return;
        float vol = Math.max(0.5f, cfg.soundVolume) * (0.7f + intensity * 0.5f);
        play(SoundEvents.MACE_SMASH_GROUND_HEAVY, 0.85f + intensity * 0.15f, vol);
        play(SoundEvents.GENERIC_EXPLODE.value(), 0.9f, vol * 0.8f);
    }

    private static void play(SoundEvent ev, float pitch, float volume) {
        if (ev == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        float p = Math.max(0.5f, Math.min(2.0f, pitch));
        // Vanilla order is forUI(sound, pitch, volume).
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ev, p, Math.max(0f, volume)));
    }
}
