package dev.limucc.brutaltyping.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import dev.limucc.brutaltyping.BrutalTyping;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** MC 1.21.9–1.21.11: keybindings registered with a {@link KeyMapping.Category}. */
public final class KeyCompat {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(BrutalTyping.MOD_ID, "main"));

    private KeyCompat() {}

    public static KeyMapping register(String translationKey) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyMapping(translationKey, InputConstants.UNKNOWN.getValue(), CATEGORY));
    }
}
