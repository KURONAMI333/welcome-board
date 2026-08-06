package com.kuronami.welcomeboard.client;

/**
 * The footprint (position + size) of the welcome-board panel, structurally split into three
 * non-overlapping strips, top to bottom:
 *
 * <ul>
 *   <li><b>text area</b> (the top {@code textAreaHeight()} pixels) — title/body/image from the
 *       pack author's content file are anchored here (via {@link
 *       com.kuronami.welcomeboard.layout.LayoutResolver}, {@code panel}-relative through {@code
 *       WelcomeBoardScreen#resolveInPanel}). Pack-author buttons are never placed in this strip —
 *       that is exactly the bug this three-band split replaces: sizing the panel tightly around
 *       the text put {@code bottom_left}/{@code bottom_right} buttons directly on top of the last
 *       body line, because the old two-band split resolved buttons against the same rectangle as
 *       the text instead of giving them their own reserved space.</li>
 *   <li><b>content-button band</b> (the next {@code buttonBandHeight} pixels, zero when the
 *       content file declares no buttons — see {@link #buttonBandHeightFor}) — pack-author
 *       buttons ({@code content.buttons()}) live here exclusively, laid out by {@code
 *       WelcomeBoardScreen#layoutButtonBand}. Fixed size, independent of the text above it, so it
 *       is never squeezed by a tall content file.</li>
 *   <li><b>close-button band</b> (the bottom {@code closeBandHeight} pixels, fixed at construction
 *       time by the caller — see {@code WelcomeBoardScreen#CLOSE_BAND_HEIGHT}) — reserved
 *       exclusively for the close button, which renders alone, centered, as the panel's one
 *       structurally-guaranteed primary action. No content-file anchor ever resolves into this
 *       band, so a pack author cannot place a button/image that collides with it, regardless of
 *       which anchor they choose.</li>
 * </ul>
 *
 * <p>{@code height} follows the content that will actually be drawn inside it ({@code
 * textAreaHeight() + buttonBandHeight + closeBandHeight}) instead of a fixed fraction of the
 * screen, clamped between a floor (so a one-line file doesn't collapse to nothing) and a
 * screen-height-fraction ceiling (so a pathological content file can't grow the panel past the
 * screen). Content taller than the ceiling is truncated by the renderer, which logs a warning
 * (DESIGN_COMPILE.md's "壊れていても画面は出す" rule extended to "too much content" the same way it
 * already covers "too little"/malformed content).
 *
 * <p>Per DESIGN_COMPILE.md §5/§6, exact pixel values here are a first cut, not a finished
 * spec — layout numbers are expected to move after further runClient passes.
 */
public record PanelGeometry(int x, int y, int width, int height, int buttonBandHeight, int closeBandHeight) {

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
     * @param width                 this panel's resolved width (from {@link #widthFor}).
     * @param desiredTextAreaHeight pixel height the caller measured as actually needed (using real
     *                              font metrics) to fit title + body + image at this width —
     *                              neither the content-button band nor the close-button band are
     *                              included; {@code buttonBandHeight}/{@code closeBandHeight}
     *                              below cover those.
     * @param buttonBandHeight      fixed height of the content-button band (see {@link
     *                              #buttonBandHeightFor}), zero when the content file has no
     *                              buttons. Always added on top of {@code desiredTextAreaHeight}
     *                              so a pack-author button can never be squeezed back into the
     *                              text area, and never squeezes the close-button band either.
     * @param closeBandHeight       fixed height of the close-button band, independent of content.
     */
    public static PanelGeometry of(int screenWidth, int screenHeight, int width,
                                    int desiredTextAreaHeight, int buttonBandHeight, int closeBandHeight) {
        int minHeight = Math.max(MIN_HEIGHT, buttonBandHeight + closeBandHeight);
        int maxHeight = Math.max(minHeight, (int) (screenHeight * MAX_HEIGHT_SCREEN_FRACTION));
        int desiredTotal = desiredTextAreaHeight + buttonBandHeight + closeBandHeight;
        int height = clamp(desiredTotal, minHeight, maxHeight);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;
        return new PanelGeometry(x, y, width, height, buttonBandHeight, closeBandHeight);
    }

    /**
     * Pure calc: height of the content-button band (pack-author buttons such as the sample
     * content file's Wiki/Discord). Zero when there are no buttons, so a content file without
     * pack-author buttons reserves no dead space for them (mirroring how the close-button band is
     * always reserved because a close button always exists). Never grows with button count: it is
     * a fixed single-row strip regardless of how many buttons the pack author declares — {@code
     * WelcomeBoardScreen#layoutButtonBand} packs every button onto that one row, or drops overflow
     * (with a logged warning) rather than wrapping onto a second row.
     *
     * @param buttonCount     number of pack-author buttons in the content file.
     * @param buttonHeight    height of a single button widget.
     * @param verticalMargin  margin reserved above and below the button row (mirrors the
     *                        close-button band's own margin, for the same visual rhythm).
     * @param dividerGapAbove gap between the text area and this band's leading divider rule.
     * @param dividerHeight   height of the divider rule itself.
     */
    public static int buttonBandHeightFor(int buttonCount, int buttonHeight, int verticalMargin,
                                           int dividerGapAbove, int dividerHeight) {
        if (buttonCount <= 0) {
            return 0;
        }
        return dividerGapAbove + dividerHeight + verticalMargin * 2 + buttonHeight;
    }

    /**
     * Pure calc: x-offset (relative to the content-button band's left edge, i.e. in {@code
     * [0, bandWidth]}) where the center-anchored button block should start.
     *
     * <p>Naively centering the block in the <em>whole</em> band (ignoring the left/right zones)
     * is exactly the bug this method exists to prevent: the total-width feasibility check ({@code
     * WelcomeBoardScreen#layoutButtonBand}'s drop loop) guarantees {@code leftWidth + centerWidth
     * + rightWidth} plus inter-zone gaps fits inside {@code bandWidth}, but a plain "{@code
     * (bandWidth - centerWidth) / 2}" placement does not account for where the left/right blocks
     * actually sit — a wide-enough left block can still collide with a naively-centered block even
     * though every zone individually fits. This clamps the naive center point away from whichever
     * zone(s) are non-empty (leaving {@code gap} pixels of breathing room), so it can only ever
     * move the block toward the band's true empty middle, never off past a zone that has content.
     */
    public static int centerBlockStart(int bandWidth, int leftWidth, int centerWidth, int rightWidth, int gap) {
        int naiveStart = Math.max(0, (bandWidth - centerWidth) / 2);
        int minStart = leftWidth > 0 ? leftWidth + gap : 0;
        int maxStart = rightWidth > 0 ? bandWidth - rightWidth - gap - centerWidth : bandWidth - centerWidth;
        if (minStart > maxStart) {
            // Only reachable if leftWidth + centerWidth + rightWidth (+ gaps) already exceeds
            // bandWidth — callers are expected to have dropped overflow buttons first so this
            // shouldn't happen in practice, but favor not overlapping the left zone if it does.
            return minStart;
        }
        return clamp(naiveStart, minStart, maxStart);
    }

    /**
     * Height of the text area (title/body/image — everything above the content-button band and
     * the close-button band). Never negative.
     */
    public int textAreaHeight() {
        return Math.max(0, height - buttonBandHeight - closeBandHeight);
    }

    /** Y coordinate where the content-button band starts (== bottom edge of the text area). */
    public int buttonBandY() {
        return y + textAreaHeight();
    }

    /**
     * Y coordinate where the close-button band starts (== bottom edge of the content-button
     * band, which sits flush against the text area's bottom edge when there are no buttons).
     */
    public int closeBandY() {
        return y + textAreaHeight() + buttonBandHeight;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
