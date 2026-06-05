package dev.limucc.brutaltyping.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.limucc.brutaltyping.BrutalTyping;
import dev.limucc.brutaltyping.client.config.BrutalConfigManager;
import dev.limucc.brutaltyping.client.engine.EffectEngine;
import dev.limucc.brutaltyping.client.gui.BrutalTypingScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class BrutalTypingClient implements ClientModInitializer {

    /** Options → Controls → Brutal Typing. */
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(BrutalTyping.MOD_ID, "main"));

    public static KeyMapping OPEN_SETTINGS_KEY;
    public static KeyMapping TOGGLE_KEY;

    @Override
    public void onInitializeClient() {
        BrutalConfigManager.load();

        OPEN_SETTINGS_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.brutaltyping.open_settings", InputConstants.UNKNOWN.getValue(), CATEGORY));
        TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.brutaltyping.toggle", InputConstants.UNKNOWN.getValue(), CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            EffectEngine.INSTANCE.tick();   // cool the amplifier down + drain stale combos every tick

            while (OPEN_SETTINGS_KEY.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new BrutalTypingScreen(null));
            }
            while (TOGGLE_KEY.consumeClick()) {
                var c = BrutalConfigManager.get();
                c.enabled = !c.enabled;
                BrutalConfigManager.save();
                BrutalTyping.LOGGER.info("Brutal Typing {}.", c.enabled ? "ENABLED" : "disabled");
            }
        });

        BrutalTyping.LOGGER.info("Brutal Typing client ready. Open settings via ModMenu or bind a key under Options → Controls → Brutal Typing.");
    }
}
