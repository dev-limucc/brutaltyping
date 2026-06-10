package dev.limucc.brutaltyping.client.compat;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** Early 1.21.x: sound events whose holder-ness differs across versions — ITEM_BREAK is still plain here. */
public final class SoundCompat {

    private SoundCompat() {}

    public static SoundEvent itemBreak() {
        return SoundEvents.ITEM_BREAK;
    }
}
