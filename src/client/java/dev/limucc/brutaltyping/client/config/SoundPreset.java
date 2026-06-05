package dev.limucc.brutaltyping.client.config;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Supplier;

/**
 * The "voice" of your typing — which vanilla sound each keystroke makes. {@code primary} fires per keystroke
 * (pitch scales with the amplifier); {@code milestone} fires on combo milestones. {@code GENERIC_EXPLODE} is a
 * {@code Holder.Reference}, so we resolve it with {@code .value()}; the rest are plain {@code SoundEvent}s.
 */
public enum SoundPreset {
    TNT      ("TNT Explosion", () -> SoundEvents.GENERIC_EXPLODE.value(), () -> SoundEvents.DRAGON_FIREBALL_EXPLODE),
    DRAGON   ("Dragon",        () -> SoundEvents.DRAGON_FIREBALL_EXPLODE, () -> SoundEvents.ENDER_DRAGON_DEATH),
    WITHER   ("Wither",        () -> SoundEvents.WITHER_HURT,             () -> SoundEvents.WITHER_SPAWN),
    LIGHTNING("Lightning",     () -> SoundEvents.LIGHTNING_BOLT_IMPACT,   () -> SoundEvents.LIGHTNING_BOLT_THUNDER),
    FIREWORK ("Firework",      () -> SoundEvents.FIREWORK_ROCKET_BLAST,   () -> SoundEvents.FIREWORK_ROCKET_LARGE_BLAST),
    ANVIL    ("Anvil",         () -> SoundEvents.ANVIL_LAND,              () -> SoundEvents.ANVIL_DESTROY),
    BLIP     ("XP Blip",       () -> SoundEvents.EXPERIENCE_ORB_PICKUP,   () -> SoundEvents.PLAYER_LEVELUP),
    MUTE     ("Mute",          null,                                     null);

    public final String display;
    private final Supplier<SoundEvent> primary;
    private final Supplier<SoundEvent> milestone;

    SoundPreset(String display, Supplier<SoundEvent> primary, Supplier<SoundEvent> milestone) {
        this.display = display;
        this.primary = primary;
        this.milestone = milestone;
    }

    public boolean muted() { return primary == null; }

    /** Resolved lazily so the sound registry is guaranteed to be bootstrapped by the time we play. */
    public SoundEvent primary()   { return primary == null   ? null : primary.get(); }
    public SoundEvent milestone() { return milestone == null ? null : milestone.get(); }

    public SoundPreset next() { return values()[(ordinal() + 1) % values().length]; }
}
