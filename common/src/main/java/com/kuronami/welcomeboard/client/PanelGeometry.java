package com.kuronami.welcomeboard.client;

import com.kuronami.welcomeboard.config.LayoutPreset;

/**
 * The footprint (position + size) of the title/body/close panel for each {@link LayoutPreset}.
 *
 * <p>Deliberately <em>not</em> resolved through {@link com.kuronami.welcomeboard.layout.LayoutResolver}:
 * that class resolves a pack author's per-element {@code anchor}/{@code offset}. As of the ①/②
 * fixes below, {@code LayoutResolver} is still used for the optional image and the buttons, but
 * the rectangle its callers pass is now this panel's own {@code width()}/{@code height()} (with the
 * result translated by {@code panel.x()}/{@code panel.y()}) rather than the full screen — the
 * content JSON's anchors are panel-relative, not screen-relative, for any preset that has a panel
 * (all three do). The panel itself is a player-facing display choice (DESIGN_COMPILE.md U6), not
 * something the content file controls, so its geometry stays preset-driven.
 *
 * <p>{@code height} follows the content that will actually be drawn inside it (title lines + body
 * lines + image + the close-button row + padding — see {@code WelcomeBoardScreen#estimateContentHeight})
 * instead of a fixed fraction of the screen, clamped between a preset-specific floor (so a
 * one-line file doesn't collapse to nothing) and a screen-height-fraction ceiling (so a pathological
 * content file can't grow the panel past the screen). Content taller than the ceiling is truncated
 * by the renderer, which logs a warning (DESIGN_COMPILE.md's "壊れていても画面は出す" rule extended to
 * "too much content" the same way it already covers "too little"/malformed content).
 *
 * <p>Per DESIGN_COMPILE.md §5/§6, exact pixel values here are a first cut, not a finished
 * spec — "レイアウト数値は確定させない... プリセット3案を作って kura に1回で見比べさせる". Expect these
 * numbers to move after the runClient candidate-sheet gate.
 */
public record PanelGeometry(int x, int y, int width, int height) {

    private static final double CARD_WIDTH_FRACTION = 0.6;
    private static final int MIN_CARD_WIDTH = 240;
    private static final int MAX_CARD_WIDTH = 380;
    // Floor only exists so a near-empty file (title, no body) doesn't collapse the panel to
    // nothing — it must stay below any real content's estimated height or it silently reintroduces
    // defect ②'s dead space (140 — this constant's value when height was still a fixed screen
    // fraction — was exactly that bug: WelcomeBoardScreen#estimateContentHeight's minimum case
    // (title + close row + padding, no body) comes out around 68px, so 140 clamped every typical
    // file straight back to the old fixed height).
    private static final int MIN_CARD_HEIGHT = 80;
    private static final double CARD_MAX_HEIGHT_SCREEN_FRACTION = 0.75;

    private static final int BANNER_MARGIN = 20;
    private static final int MIN_BANNER_HEIGHT = 60;
    private static final double BANNER_MAX_HEIGHT_SCREEN_FRACTION = 0.35;
    private static final int BANNER_TOP_OFFSET = 24;

    private static final int FULL_MARGIN = 20;

    /**
     * Preset width alone, independent of content — needed by the caller *before* it can measure
     * how tall the content will wrap to (word-wrap width depends on the panel's width).
     */
    public static int widthFor(LayoutPreset preset, int screenWidth) {
        return switch (preset) {
            case CARD -> clamp((int) (screenWidth * CARD_WIDTH_FRACTION), MIN_CARD_WIDTH, MAX_CARD_WIDTH);
            case BANNER -> Math.max(240, screenWidth - BANNER_MARGIN * 2);
            case FULL -> Math.max(240, screenWidth - FULL_MARGIN * 2);
        };
    }

    /**
     * @param desiredContentHeight pixel height the caller measured as actually needed (using real
     *                             font metrics) to fit title + body + close-button row + image at
     *                             this preset's resolved width. Ignored by {@code FULL}: a
     *                             mostly-fullscreen takeover is that preset's entire point
     *                             (see {@link LayoutPreset#FULL}'s javadoc), so it does not shrink
     *                             to content the way {@code CARD}/{@code BANNER} do.
     */
    public static PanelGeometry forPreset(LayoutPreset preset, int screenWidth, int screenHeight, int desiredContentHeight) {
        int width = widthFor(preset, screenWidth);
        return switch (preset) {
            case CARD -> card(screenWidth, screenHeight, width, desiredContentHeight);
            case BANNER -> banner(screenWidth, screenHeight, width, desiredContentHeight);
            case FULL -> full(screenWidth, screenHeight, width);
        };
    }

    private static PanelGeometry card(int screenWidth, int screenHeight, int width, int desiredContentHeight) {
        int maxHeight = Math.max(MIN_CARD_HEIGHT, (int) (screenHeight * CARD_MAX_HEIGHT_SCREEN_FRACTION));
        int height = clamp(desiredContentHeight, MIN_CARD_HEIGHT, maxHeight);
        return new PanelGeometry((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }

    private static PanelGeometry banner(int screenWidth, int screenHeight, int width, int desiredContentHeight) {
        int maxHeight = Math.max(MIN_BANNER_HEIGHT, (int) (screenHeight * BANNER_MAX_HEIGHT_SCREEN_FRACTION));
        int height = clamp(desiredContentHeight, MIN_BANNER_HEIGHT, maxHeight);
        return new PanelGeometry(BANNER_MARGIN, BANNER_TOP_OFFSET, width, height);
    }

    private static PanelGeometry full(int screenWidth, int screenHeight, int width) {
        int height = Math.max(160, screenHeight - FULL_MARGIN * 2);
        return new PanelGeometry(FULL_MARGIN, FULL_MARGIN, width, height);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
