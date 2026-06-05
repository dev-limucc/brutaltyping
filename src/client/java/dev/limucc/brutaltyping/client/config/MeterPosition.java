package dev.limucc.brutaltyping.client.config;

/** Where the (optional) combo/WPM meter is drawn. Kept small and out of the way. */
public enum MeterPosition {
    ABOVE_CHAT("Above Chat"),
    BOTTOM_LEFT("Bottom Left"),
    TOP_LEFT("Top Left"),
    TOP_RIGHT("Top Right");

    public final String display;

    MeterPosition(String display) { this.display = display; }

    public MeterPosition next() { return values()[(ordinal() + 1) % values().length]; }
}
