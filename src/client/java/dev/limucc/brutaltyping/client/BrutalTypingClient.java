package dev.limucc.brutaltyping.client;

import dev.limucc.brutaltyping.BrutalTyping;
import dev.limucc.brutaltyping.client.compat.KeyCompat;
import dev.limucc.brutaltyping.client.config.BrutalConfigManager;
import dev.limucc.brutaltyping.client.engine.EffectEngine;
import dev.limucc.brutaltyping.client.gui.BrutalTypingScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;

public class BrutalTypingClient implements ClientModInitializer {

    public static KeyMapping OPEN_SETTINGS_KEY;
    public static KeyMapping TOGGLE_KEY;

    @Override
    public void onInitializeClient() {
        BrutalConfigManager.load();

        // Options → Controls → Brutal Typing. Registration differs per MC version, hence KeyCompat.
        OPEN_SETTINGS_KEY = KeyCompat.register("key.brutaltyping.open_settings");
        TOGGLE_KEY = KeyCompat.register("key.brutaltyping.toggle");

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
