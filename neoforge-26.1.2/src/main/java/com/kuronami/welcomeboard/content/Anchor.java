package com.kuronami.welcomeboard.content;

import java.util.Locale;

/**
 * Named screen positions for an element (image or button). No Minecraft types allowed here:
 * this class belongs to the calc layer and must remain compilable/testable without the game
 * on the classpath.
 */
public enum Anchor {
    TOP,
    TOP_LEFT,
    TOP_RIGHT,
    CENTER,
    LEFT,
    RIGHT,
    BOTTOM,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    /** Fallback used whenever the pack author's value is missing or unrecognized. */
    public static final Anchor DEFAULT = CENTER;

    /**
     * Parses a content-file anchor string ("bottom_left", "Bottom-Left", ...).
     * Never throws: returns {@code null} for anything it can't recognize so the caller
     * can decide how to warn and fall back.
     */
    public static Anchor fromString(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (Anchor anchor : values()) {
            if (anchor.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return anchor;
            }
        }
        return null;
    }
}
