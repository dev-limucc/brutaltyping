package dev.limucc.brutaltyping.client.compat;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** MC 26.1+: sound events whose holder-ness differs across versions. */
public final class SoundCompat {

    private SoundCompat() {}

    public static SoundEvent itemBreak() {
        return SoundEvents.ITEM_BREAK.value();
    }
}
