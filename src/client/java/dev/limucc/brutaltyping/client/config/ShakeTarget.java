package dev.limucc.brutaltyping.client.config;

/** What the screen-shake is allowed to rattle. */
public enum ShakeTarget {
    SCREEN("Whole Screen"),
    CHAT_ONLY("Chatbox Only"),
    OFF("Off");

    public final String display;

    ShakeTarget(String display) { this.display = display; }

    public ShakeTarget next() { return values()[(ordinal() + 1) % values().length]; }
}
