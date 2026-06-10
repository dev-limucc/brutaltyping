package dev.limucc.brutaltyping.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

/** MC 1.21–1.21.8: keybindings registered with a translation-key category string. */
public final class KeyCompat {

    private static final String CATEGORY = "key.categories.brutaltyping";

    private KeyCompat() {}

    public static KeyMapping register(String translationKey) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyMapping(translationKey, InputConstants.UNKNOWN.getValue(), CATEGORY));
    }
}
