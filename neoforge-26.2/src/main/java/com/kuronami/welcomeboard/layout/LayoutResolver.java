package com.kuronami.welcomeboard.layout;

import com.kuronami.welcomeboard.content.Anchor;

/**
 * Resolves an anchor + offset into an actual top-left screen coordinate, clamped so the
 * element never renders off-screen. This is the entire reason pack authors write
 * {@code "anchor": "bottom_left"} instead of raw pixel coordinates. Calc layer: no
 * Minecraft types are used here, so it is fully unit-testable without the game.
 */
public final class LayoutResolver {

    private LayoutResolver() {
    }

    /**
     * @param screenWidth    current screen width in pixels
     * @param screenHeight   current screen height in pixels
     * @param elementWidth   intended width of the element being placed
     * @param elementHeight  intended height of the element being placed
     * @param anchor         named anchor point; falls back to {@link Anchor#DEFAULT} if null
     * @param offsetX        pack-author offset applied after anchor resolution
     * @param offsetY        pack-author offset applied after anchor resolution
     * @return the top-left coordinate of the element, clamped to stay within the screen
     *         whenever the element itself is not larger than the screen.
     */
    public static Point resolve(int screenWidth, int screenHeight, int elementWidth, int elementHeight,
                                 Anchor anchor, int offsetX, int offsetY) {
        Anchor effectiveAnchor = anchor != null ? anchor : Anchor.DEFAULT;

        int baseX = switch (effectiveAnchor) {
            case TOP_LEFT, LEFT, BOTTOM_LEFT -> 0;
            case TOP, CENTER, BOTTOM -> (screenWidth - elementWidth) / 2;
            case TOP_RIGHT, RIGHT, BOTTOM_RIGHT -> screenWidth - elementWidth;
        };

        int baseY = switch (effectiveAnchor) {
            case TOP_LEFT, TOP, TOP_RIGHT -> 0;
            case LEFT, CENTER, RIGHT -> (screenHeight - elementHeight) / 2;
            case BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT -> screenHeight - elementHeight;
        };

        int x = clamp(baseX + offsetX, screenWidth, elementWidth);
        int y = clamp(baseY + offsetY, screenHeight, elementHeight);
        return new Point(x, y);
    }

    private static int clamp(int value, int screenSize, int elementSize) {
        int max = Math.max(0, screenSize - elementSize);
        if (value < 0) {
            return 0;
        }
        return Math.min(value, max);
    }
}
