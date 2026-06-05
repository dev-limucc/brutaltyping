package dev.limucc.brutaltyping.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.limucc.brutaltyping.client.gui.BrutalTypingScreen;

/** Opens the Brutal Typing settings GUI from the ModMenu mod list. */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BrutalTypingScreen::new;
    }
}
