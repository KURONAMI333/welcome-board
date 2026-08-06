package com.kuronami.welcomeboard.layout;

import com.kuronami.welcomeboard.content.Anchor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutResolverTest {

    private static final int ELEMENT_WIDTH = 100;
    private static final int ELEMENT_HEIGHT = 50;

    // ---- positive: every anchor resolves to the expected coordinate, at three resolutions ----

    @Test
    void resolvesAllAnchorsAt320x240() {
        assertAnchorGrid(320, 240, 110, 95);
    }

    @Test
    void resolvesAllAnchorsAt854x480() {
        assertAnchorGrid(854, 480, 377, 215);
    }

    @Test
    void resolvesAllAnchorsAt1920x1080() {
        assertAnchorGrid(1920, 1080, 910, 515);
    }

    /**
     * @param midX expected x for the horizontally-centered anchors (TOP/CENTER/BOTTOM) = (screenWidth - ELEMENT_WIDTH) / 2
     * @param midY expected y for the vertically-centered anchors (LEFT/CENTER/RIGHT) = (screenHeight - ELEMENT_HEIGHT) / 2
     */
    private void assertAnchorGrid(int screenWidth, int screenHeight, int midX, int midY) {
        int maxX = screenWidth - ELEMENT_WIDTH;
        int maxY = screenHeight - ELEMENT_HEIGHT;

        assertResolved(screenWidth, screenHeight, Anchor.TOP_LEFT, 0, 0);
        assertResolved(screenWidth, screenHeight, Anchor.TOP, midX, 0);
        assertResolved(screenWidth, screenHeight, Anchor.TOP_RIGHT, maxX, 0);
        assertResolved(screenWidth, screenHeight, Anchor.LEFT, 0, midY);
        assertResolved(screenWidth, screenHeight, Anchor.CENTER, midX, midY);
        assertResolved(screenWidth, screenHeight, Anchor.RIGHT, maxX, midY);
        assertResolved(screenWidth, screenHeight, Anchor.BOTTOM_LEFT, 0, maxY);
        assertResolved(screenWidth, screenHeight, Anchor.BOTTOM, midX, maxY);
        assertResolved(screenWidth, screenHeight, Anchor.BOTTOM_RIGHT, maxX, maxY);
    }

    private void assertResolved(int screenWidth, int screenHeight, Anchor anchor, int expectedX, int expectedY) {
        Point point = LayoutResolver.resolve(screenWidth, screenHeight, ELEMENT_WIDTH, ELEMENT_HEIGHT, anchor, 0, 0);
        assertEquals(new Point(expectedX, expectedY), point, "anchor " + anchor + " at " + screenWidth + "x" + screenHeight);
    }

    // ---- negative: never renders off-screen ----

    @Test
    void hugePositiveOffsetClampsToBottomRightOfScreen() {
        Point point = LayoutResolver.resolve(320, 240, ELEMENT_WIDTH, ELEMENT_HEIGHT, Anchor.CENTER, 100_000, 100_000);
        assertEquals(new Point(320 - ELEMENT_WIDTH, 240 - ELEMENT_HEIGHT), point);
    }

    @Test
    void hugeNegativeOffsetClampsToTopLeftOfScreen() {
        Point point = LayoutResolver.resolve(320, 240, ELEMENT_WIDTH, ELEMENT_HEIGHT, Anchor.CENTER, -100_000, -100_000);
        assertEquals(new Point(0, 0), point);
    }

    @Test
    void elementLargerThanScreenStaysNonNegative() {
        // element bigger than the screen in both dimensions: there's no valid "inside" position,
        // but the anchor point itself must never go negative.
        Point point = LayoutResolver.resolve(320, 240, 500, 500, Anchor.CENTER, 0, 0);
        assertEquals(0, point.x());
        assertEquals(0, point.y());
    }

    @Test
    void nullAnchorFallsBackToDefaultWithoutThrowing() {
        Point withNull = LayoutResolver.resolve(320, 240, ELEMENT_WIDTH, ELEMENT_HEIGHT, null, 0, 0);
        Point withDefault = LayoutResolver.resolve(320, 240, ELEMENT_WIDTH, ELEMENT_HEIGHT, Anchor.DEFAULT, 0, 0);
        assertEquals(withDefault, withNull);
    }

    @Test
    void resolvedPositionNeverExceedsScreenBounds() {
        for (Anchor anchor : Anchor.values()) {
            Point point = LayoutResolver.resolve(320, 240, ELEMENT_WIDTH, ELEMENT_HEIGHT, anchor, 5_000, -5_000);
            assertTrue(point.x() >= 0 && point.x() <= 320 - ELEMENT_WIDTH, anchor + " x out of bounds: " + point);
            assertTrue(point.y() >= 0 && point.y() <= 240 - ELEMENT_HEIGHT, anchor + " y out of bounds: " + point);
        }
    }
}
