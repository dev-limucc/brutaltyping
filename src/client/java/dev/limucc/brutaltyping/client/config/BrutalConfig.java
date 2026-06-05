package dev.limucc.brutaltyping.client.config;

/**
 * All Brutal Typing settings, grouped by the tab they appear under. Plain public fields so GSON round-trips
 * them trivially (enums are stored by name). Defaults are tuned to feel punchy but not nauseating.
 */
public class BrutalConfig {

    // ── General ──────────────────────────────────────────────────────────────────
    public boolean enabled = true;
    public boolean inChat = true;          // fire effects while typing in the chat box
    public boolean inTextFields = true;    // fire in other text fields (anvil, world naming, settings, search, ...)
    public boolean onDelete = true;        // heavier (and varied) "boom" on backspace / delete
    public boolean showComboMeter = false; // draw the combo / amplifier meter (hidden by default)
    public MeterPosition meterPosition = MeterPosition.ABOVE_CHAT;

    // ── Voice (sound) ────────────────────────────────────────────────────────────
    public SoundPreset soundPreset = SoundPreset.TNT;
    public float soundVolume = 0.8f;       // 0.0 .. 1.5
    public float pitchMin = 0.7f;          // pitch when cold
    public float pitchMax = 1.7f;          // pitch when fully amplified
    public boolean milestoneSounds = true; // bigger sound on combo milestones

    // ── Screen (shake) ───────────────────────────────────────────────────────────
    public ShakeTarget shakeTarget = ShakeTarget.SCREEN;
    public float shakeIntensity = 1.0f;    // 0.0 .. 3.0 multiplier
    public float shakeDecay = 12.0f;       // how fast the rattle settles (per second)

    // ── Brutality (particles / animations) ───────────────────────────────────────
    public boolean brutalLetters = true;   // typed glyphs fly off and burn
    public boolean sparks = true;
    public boolean fire = true;            // orange/gold fire particles
    public boolean soulFire = true;        // cyan soul-fire at high heat
    public boolean explosions = true;      // TNT flash + shockwave + smoke + debris on milestones
    public boolean screenFlash = true;     // white flash on big hits
    public boolean capsImportance = true;  // ALL-CAPS words (NOT, STOP) get bigger, fierier, "important" letters
    public float particleAmount = 1.0f;    // 0.0 .. 2.0 global count multiplier
    public float particleSize = 1.0f;      // 0.0 .. 2.0
    public float gravity = 1.0f;           // 0.0 .. 2.0

    // ── Amplifier (the dopamine engine) ──────────────────────────────────────────
    public float sensitivity = 0.16f;      // heat gained per fast keystroke (0.03 .. 0.5)
    public float coolDown = 0.85f;         // heat lost per second when idle (0.2 .. 3.0)
    public float strength = 2.5f;          // max effect multiplier at full heat (1.0 .. 5.0)
    public int comboWindowMs = 1200;       // keep typing within this to hold your combo (400 .. 3000)
    public boolean wpmCounter = true;      // show live WPM on the meter

    // ── Exclusivity (the flex) ───────────────────────────────────────────────────
    public boolean crit = true;            // crit stars at high heat
    public boolean sharpness = true;       // enchant sparkles at high heat
    public boolean fireworks = true;       // firework burst on milestones
    public boolean rainbowAtMax = true;    // rainbow letters in the APOCALYPSE tier
    public int milestoneEvery = 25;        // combo count between big celebrations (5 .. 100)

    public boolean weaponDrops = true;     // random weapon textures fall in with their own sounds
    public float weaponDropChance = 0.06f; // per-keystroke chance (0.0 .. 0.5)
    public boolean impactFrames = true;    // small chance the spacebar triggers a comic impact frame
    public float impactFrameChance = 0.06f;// per-space chance (0.0 .. 0.5)
    public boolean sendSlam = true;        // on send, a weapon smashes your message into the chat
}
