package dev.limucc.brutaltyping.client.engine;

/** The amplifier ladder. As {@code heat} (0..1) climbs, you move up tiers — each louder and more colourful. */
public enum Tier {
    COLD      (0.00f, "COLD",       0xFFB0B0B0),
    WARM      (0.15f, "WARM",       0xFFFFE066),
    HOT       (0.35f, "HOT",        0xFFFFC020),
    BRUTAL    (0.55f, "BRUTAL",     0xFFFF7A1A),
    INFERNO   (0.75f, "INFERNO",    0xFFFF3320),
    APOCALYPSE(0.92f, "APOCALYPSE", 0xFFFFFFFF);

    public final float min;
    public final String label;
    public final int color;

    Tier(float min, String label, int color) {
        this.min = min;
        this.label = label;
        this.color = color;
    }

    public static Tier of(float heat) {
        Tier result = COLD;
        for (Tier t : values()) {
            if (heat >= t.min) result = t;
        }
        return result;
    }
}
