package dev.limucc.brutaltyping.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.limucc.brutaltyping.BrutalTyping;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads/saves {@link BrutalConfig} to {@code config/brutaltyping.json}. Mirrors Trashventory's pattern. */
public final class BrutalConfigManager {

    private BrutalConfigManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("brutaltyping.json");

    private static BrutalConfig instance = new BrutalConfig();

    public static BrutalConfig get() {
        return instance;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            BrutalConfig loaded = GSON.fromJson(r, BrutalConfig.class);
            instance = (loaded != null) ? loaded : new BrutalConfig();
        } catch (Exception e) {
            BrutalTyping.LOGGER.error("Failed to load Brutal Typing config; using defaults.", e);
            instance = new BrutalConfig();
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            BrutalTyping.LOGGER.error("Failed to save Brutal Typing config.", e);
        }
    }
}
