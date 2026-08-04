package com.kuronami.welcomeboard.client;

import com.kuronami.welcomeboard.config.LayoutPreset;

/**
 * The footprint (position + size) of the title/body/close panel for each {@link LayoutPreset}.
 *
 * <p>Deliberately <em>not</em> resolved through {@link com.kuronami.welcomeboard.layout.LayoutResolver}:
 * that class resolves a pack author's per-element {@code anchor}/{@code offset} (used here only
 * for the optional image and the buttons, which the content JSON positions independently). The
 * panel itself is a player-facing display choice (DESIGN_COMPILE.md U6), not something the content
 * file controls, so its geometry is preset-driven instead.
 *
 * <p>Per DESIGN_COMPILE.md §5/§6, exact pixel values here are a first cut, not a finished
 * spec — "レイアウト数値は確定させない... プリセット3案を作って kura に1回で見比べさせる". Expect these
 * numbers to move after the runClient candidate-sheet gate.
 */
public record PanelGeometry(int x, int y, int width, int height) {

    private static final int MIN_CARD_WIDTH = 220;
    private static final int MAX_CARD_WIDTH = 320;
    private static final int MIN_CARD_HEIGHT = 140;
    private static final int MAX_CARD_HEIGHT = 220;

    private static final int BANNER_MARGIN = 20;
    private static final int MIN_BANNER_HEIGHT = 60;
    private static final int MAX_BANNER_HEIGHT = 100;
    private static final int BANNER_TOP_OFFSET = 24;

    private static final int FULL_MARGIN = 20;

    public static PanelGeometry forPreset(LayoutPreset preset, int screenWidth, int screenHeight) {
        return switch (preset) {
            case CARD -> card(screenWidth, screenHeight);
            case BANNER -> banner(screenWidth, screenHeight);
            case FULL -> full(screenWidth, screenHeight);
        };
    }

    private static PanelGeometry card(int screenWidth, int screenHeight) {
        int width = clamp((int) (screenWidth * 0.5), MIN_CARD_WIDTH, MAX_CARD_WIDTH);
        int height = clamp((int) (screenHeight * 0.55), MIN_CARD_HEIGHT, MAX_CARD_HEIGHT);
        return new PanelGeometry((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }

    private static PanelGeometry banner(int screenWidth, int screenHeight) {
        int width = Math.max(240, screenWidth - BANNER_MARGIN * 2);
        int height = clamp((int) (screenHeight * 0.22), MIN_BANNER_HEIGHT, MAX_BANNER_HEIGHT);
        return new PanelGeometry(BANNER_MARGIN, BANNER_TOP_OFFSET, width, height);
    }

    private static PanelGeometry full(int screenWidth, int screenHeight) {
        int width = Math.max(240, screenWidth - FULL_MARGIN * 2);
        int height = Math.max(160, screenHeight - FULL_MARGIN * 2);
        return new PanelGeometry(FULL_MARGIN, FULL_MARGIN, width, height);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
