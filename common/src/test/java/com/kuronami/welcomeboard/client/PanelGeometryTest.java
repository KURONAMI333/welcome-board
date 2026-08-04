package com.kuronami.welcomeboard.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PanelGeometry} has zero Minecraft types (plain ints/records), so it is directly
 * unit-testable despite living in the {@code client} package for organizational reasons — same
 * rationale as {@code layout.LayoutResolverTest}.
 */
class PanelGeometryTest {

    private static final int BUTTON_HEIGHT = 20;
    private static final int VERTICAL_MARGIN = 6;
    private static final int DIVIDER_GAP_ABOVE = 3;
    private static final int DIVIDER_HEIGHT = 1;

    // ---- buttonBandHeightFor: the pure calc WelcomeBoardScreen#init feeds into PanelGeometry#of ----

    @Test
    void zeroButtonsReserveNoBand() {
        int height = PanelGeometry.buttonBandHeightFor(0, BUTTON_HEIGHT, VERTICAL_MARGIN,
                DIVIDER_GAP_ABOVE, DIVIDER_HEIGHT);
        assertEquals(0, height);
    }

    @Test
    void negativeButtonCountAlsoReservesNoBand() {
        // Defensive: content.buttons().size() can never be negative in practice, but the function
        // should not misbehave if it ever is.
        int height = PanelGeometry.buttonBandHeightFor(-1, BUTTON_HEIGHT, VERTICAL_MARGIN,
                DIVIDER_GAP_ABOVE, DIVIDER_HEIGHT);
        assertEquals(0, height);
    }

    @Test
    void oneButtonReservesTheFixedSingleRowHeight() {
        int height = PanelGeometry.buttonBandHeightFor(1, BUTTON_HEIGHT, VERTICAL_MARGIN,
                DIVIDER_GAP_ABOVE, DIVIDER_HEIGHT);
        int expected = DIVIDER_GAP_ABOVE + DIVIDER_HEIGHT + VERTICAL_MARGIN * 2 + BUTTON_HEIGHT;
        assertEquals(expected, height);
        assertEquals(36, height, "documents the concrete value at WelcomeBoardScreen's real constants");
    }

    @Test
    void manyButtonsStillFitOnOneRowSoHeightDoesNotGrow() {
        int oneButtonHeight = PanelGeometry.buttonBandHeightFor(1, BUTTON_HEIGHT, VERTICAL_MARGIN,
                DIVIDER_GAP_ABOVE, DIVIDER_HEIGHT);
        int manyButtonsHeight = PanelGeometry.buttonBandHeightFor(8, BUTTON_HEIGHT, VERTICAL_MARGIN,
                DIVIDER_GAP_ABOVE, DIVIDER_HEIGHT);
        assertEquals(oneButtonHeight, manyButtonsHeight,
                "band height must stay a fixed single row regardless of button count "
                        + "(overflow buttons are packed onto the row or dropped, never wrapped)");
    }

    // ---- of(): the three-band split derives x/y/height correctly ----

    @Test
    void threeBandsStackWithoutOverlapWhenButtonsExist() {
        int buttonBandHeight = 36;
        int closeBandHeight = 36;
        int desiredTextAreaHeight = 100;

        PanelGeometry panel = PanelGeometry.of(800, 600, 300, desiredTextAreaHeight, buttonBandHeight, closeBandHeight);

        assertEquals(desiredTextAreaHeight, panel.textAreaHeight());
        assertEquals(buttonBandHeight, panel.buttonBandHeight());
        assertEquals(closeBandHeight, panel.closeBandHeight());
        assertEquals(panel.y() + panel.textAreaHeight(), panel.buttonBandY());
        assertEquals(panel.buttonBandY() + buttonBandHeight, panel.closeBandY());
        assertEquals(panel.y() + panel.height(), panel.closeBandY() + closeBandHeight);
    }

    @Test
    void buttonBandCollapsesToZeroWhenThereAreNoButtons() {
        PanelGeometry panel = PanelGeometry.of(800, 600, 300, 100, 0, 36);

        assertEquals(0, panel.buttonBandHeight());
        // Content-button band is zero-height, so its start coincides with the close band's start.
        assertEquals(panel.buttonBandY(), panel.closeBandY());
    }

    @Test
    void tinyScreenStillReservesBothFixedBandsBeforeText() {
        // Pathologically small screen: the panel's minimum height must still fit both fixed bands
        // (button + close), even if that leaves zero room for text — mirrors the pre-existing
        // close-band-only floor guarantee.
        PanelGeometry panel = PanelGeometry.of(100, 60, 100, 500, 36, 36);

        assertTrue(panel.height() >= 36 + 36);
        assertEquals(36, panel.buttonBandHeight());
        assertEquals(36, panel.closeBandHeight());
    }

    // ---- centerBlockStart: center-anchored buttons must never collide with left/right zones ----

    @Test
    void centerBlockStartsInTheMiddleOfAnEmptyBand() {
        int start = PanelGeometry.centerBlockStart(360, 0, 80, 0, 6);
        assertEquals((360 - 80) / 2, start);
    }

    @Test
    void centerBlockPushesRightOfAWideLeftZoneInsteadOfOverlappingIt() {
        // Reproduces the exact case that overlapped before this method existed: two 80px
        // left-anchored buttons (leftWidth = 80 + 6 + 80 = 166) plus one 80px center-anchored
        // button in a 360px band. Naive "(360-80)/2 = 140" would start the center block at 140,
        // 26px inside the left block's 0..166 span.
        int bandWidth = 360;
        int leftWidth = 80 + 6 + 80; // two left buttons + one inter-button gap
        int centerWidth = 80;
        int gap = 6;

        int start = PanelGeometry.centerBlockStart(bandWidth, leftWidth, centerWidth, 0, gap);

        assertTrue(start >= leftWidth + gap, "center block must start at/after the left zone's edge + gap: " + start);
        assertTrue(start + centerWidth <= bandWidth, "center block must stay inside the band: " + start);
    }

    @Test
    void centerBlockPullsLeftOfAWideRightZoneInsteadOfOverlappingIt() {
        int bandWidth = 300;
        int centerWidth = 80;
        int rightWidth = 80 + 6 + 80;
        int gap = 6;

        int start = PanelGeometry.centerBlockStart(bandWidth, 0, centerWidth, rightWidth, gap);

        assertTrue(start + centerWidth + gap <= bandWidth - rightWidth,
                "center block must end at/before the right zone's edge - gap: " + start);
        assertTrue(start >= 0, "center block must stay inside the band: " + start);
    }

    @Test
    void centerBlockSqueezesBetweenBothZonesWhenBothArePresent() {
        int bandWidth = 400;
        int leftWidth = 100;
        int centerWidth = 60;
        int rightWidth = 100;
        int gap = 6;

        int start = PanelGeometry.centerBlockStart(bandWidth, leftWidth, centerWidth, rightWidth, gap);

        assertTrue(start >= leftWidth + gap, "must clear the left zone: " + start);
        assertTrue(start + centerWidth + gap <= bandWidth - rightWidth, "must clear the right zone: " + start);
    }
}
