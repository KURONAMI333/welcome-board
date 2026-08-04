package com.kuronami.welcomeboard.client;

/**
 * The footprint (position + size) of the welcome-board panel, structurally split into two
 * non-overlapping strips:
 *
 * <ul>
 *   <li><b>content area</b> (the top {@code contentAreaHeight()} pixels) — title/body/image/buttons
 *       from the pack author's content file are anchored here (via {@link
 *       com.kuronami.welcomeboard.layout.LayoutResolver}, {@code panel}-relative through {@code
 *       WelcomeBoardScreen#resolveInPanel}), never in the close-button band below.</li>
 *   <li><b>close-button band</b> (the bottom {@code closeBandHeight} pixels, fixed at construction
 *       time by the caller — see {@code WelcomeBoardScreen#CLOSE_BAND_HEIGHT}) — reserved
 *       exclusively for the close button, which renders alone, centered, as the panel's one
 *       structurally-guaranteed primary action. No content-file anchor ever resolves into this
 *       band, so a pack author cannot place a button/image that collides with it, regardless of
 *       which anchor they choose.</li>
 * </ul>
 *
 * <p>{@code height} follows the content that will actually be drawn inside it (title lines + body
 * lines + image, i.e. {@code contentAreaHeight()}, plus the fixed {@code closeBandHeight}) instead
 * of a fixed fraction of the screen, clamped between a floor (so a one-line file doesn't collapse
 * to nothing) and a screen-height-fraction ceiling (so a pathological content file can't grow the
 * panel past the screen). Content taller than the ceiling is truncated by the renderer, which logs
 * a warning (DESIGN_COMPILE.md's "壊れていても画面は出す" rule extended to "too much content" the same
 * way it already covers "too little"/malformed content).
 *
 * <p>Per DESIGN_COMPILE.md §5/§6, exact pixel values here are a first cut, not a finished
 * spec — layout numbers are expected to move after further runClient passes.
 */
public record PanelGeometry(int x, int y, int width, int height, int closeBandHeight) {

    private static final double WIDTH_FRACTION = 0.6;
    private static final int MIN_WIDTH = 240;
    private static final int MAX_WIDTH = 380;

    // Floor only exists so a near-empty file (title, no body) doesn't collapse the panel to
    // nothing — it must stay below any real content's estimated height or it silently reintroduces
    // the dead-space bug this replaced (a fixed height clamping every typical file back down).
    private static final int MIN_HEIGHT = 80;
    private static final double MAX_HEIGHT_SCREEN_FRACTION = 0.75;

    /**
     * Panel width alone, independent of content — needed by the caller *before* it can measure
     * how tall the content will wrap to (word-wrap width depends on the panel's width).
     */
    public static int widthFor(int screenWidth) {
        return clamp((int) (screenWidth * WIDTH_FRACTION), MIN_WIDTH, MAX_WIDTH);
    }

    /**
     * @param width               this panel's resolved width (from {@link #widthFor}).
     * @param desiredContentAreaHeight pixel height the caller measured as actually needed (using
     *                            real font metrics) to fit title + body + image at this width —
     *                            the close-button band is <em>not</em> included, {@code
     *                            closeBandHeight} below covers it.
     * @param closeBandHeight     fixed height of the close-button band, independent of content.
     *                            Always added on top of {@code desiredContentAreaHeight} so the
     *                            band is never squeezed out by a tall content file.
     */
    public static PanelGeometry of(int screenWidth, int screenHeight, int width,
                                    int desiredContentAreaHeight, int closeBandHeight) {
        int minHeight = Math.max(MIN_HEIGHT, closeBandHeight);
        int maxHeight = Math.max(minHeight, (int) (screenHeight * MAX_HEIGHT_SCREEN_FRACTION));
        int desiredTotal = desiredContentAreaHeight + closeBandHeight;
        int height = clamp(desiredTotal, minHeight, maxHeight);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;
        return new PanelGeometry(x, y, width, height, closeBandHeight);
    }

    /** Height of the content area (everything above the close-button band). Never negative. */
    public int contentAreaHeight() {
        return Math.max(0, height - closeBandHeight);
    }

    /** Y coordinate where the close-button band starts (== bottom edge of the content area). */
    public int closeBandY() {
        return y + contentAreaHeight();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
