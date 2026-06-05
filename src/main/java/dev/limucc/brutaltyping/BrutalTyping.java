package dev.limucc.brutaltyping;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. Brutal Typing is a purely client-side, for-fun mod: all the interesting work lives in
 * the client source set ({@code dev.limucc.brutaltyping.client}). This class only exists so the mod has a
 * valid "main" entrypoint and a shared logger / id.
 */
public class BrutalTyping implements ModInitializer {

    public static final String MOD_ID = "brutaltyping";
    public static final Logger LOGGER = LoggerFactory.getLogger("Brutal Typing");

    @Override
    public void onInitialize() {
        LOGGER.info("Brutal Typing loaded — go type something violent.");
    }
}
