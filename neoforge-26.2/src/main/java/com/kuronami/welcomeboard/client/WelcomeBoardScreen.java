package com.kuronami.welcomeboard.client;

import com.kuronami.welcomeboard.Constants;
import com.kuronami.welcomeboard.content.Anchor;
import com.kuronami.welcomeboard.content.WelcomeButton;
import com.kuronami.welcomeboard.content.WelcomeContent;
import com.kuronami.welcomeboard.content.WelcomeImage;
import com.kuronami.welcomeboard.layout.LayoutResolver;
import com.kuronami.welcomeboard.layout.Point;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws one welcome-board content file (DESIGN_COMPILE.md U4): a single centered modal card,
 * structurally split into three non-overlapping bands (see {@link PanelGeometry}), top to
 * bottom:
 *
 * <ol>
 *   <li>the <b>text area</b> — title/body/image (see {@link #renderPanelContents}), the only band
 *       {@link #resolveInPanel} ever resolves an anchor against;</li>
 *   <li>the <b>content-button band</b> — pack-author buttons ({@code content.buttons()}, see
 *       {@link #layoutButtonBand}), laid out left/center/right by the horizontal component of
 *       each button's anchor (see {@link #horizontalAlignOf});</li>
 *   <li>the <b>close-button band</b> — the panel's one structurally-guaranteed primary action
 *       (see {@link #addCloseButton}).</li>
 * </ol>
 *
 * <p>Each band owns its own vertical space and none of them share it, so a pack author's own
 * button can never land on top of their own body text or the close button, regardless of which
 * anchor they choose — that reachability is exactly what made the two-band layout this replaces
 * capable of rendering a {@code bottom_left}/{@code bottom_right} button directly on top of the
 * last body line (the panel shrank to fit the text exactly, so "bottom of the content area" and
 * "last line of text" were the same pixel). It remains possible for a pack author's own body text
 * to visually run long inside the text area's own scroll-free clamp (see {@link
 * #renderPanelContents}); that is a separate, pre-existing truncation behavior, not a collision.
 *
 * <p>URL buttons never open a link directly (DESIGN_COMPILE.md's constraint): they push vanilla's
 * {@link ConfirmLinkScreen} first, same as every other in-game "open this link?" prompt, and
 * return to this screen afterward regardless of the player's answer.
 *
 * <p><b>Draw phases.</b> Minecraft 26.x extracts a screen in two passes separated by a stratum
 * boundary: {@link #extractBackground} first, then {@link #extractRenderState} (which draws every
 * widget added by {@link #addCloseButton}/{@link #layoutButtonBand}). Submission order <em>within</em>
 * one stratum is not a depth guarantee, so every piece of panel chrome that must sit behind the
 * buttons — the panel fill, its outline, and all three divider rules — is emitted in {@link
 * #extractBackground}. {@link #extractRenderState} is left with only the things that draw on top of
 * nothing: the title/body text and the optional image, both of which live inside the text area,
 * which no widget ever occupies.
 */
public final class WelcomeBoardScreen extends Screen {

    private static final int PADDING = 10;
    private static final int LINE_SPACING = 1;
    private static final int BUTTON_HEIGHT = 20;

    // Gap above/below the title-divider rule (title needed a real break from the body, not just a
    // color difference nobody notices against a dark panel — see #renderPanelContents).
    private static final int TITLE_RULE_GAP_ABOVE = 3;
    private static final int TITLE_RULE_GAP_BELOW = 4;
    private static final int TITLE_RULE_HEIGHT = 1;

    // Content-button band: a fixed-size strip, reserved only when the content file declares at
    // least one button (PanelGeometry.buttonBandHeightFor), sitting between the text area and the
    // close-button band. Same divider visual language as the title rule and the close-band
    // divider below, so all three bands read as one consistent panel rather than three pasted-
    // together pieces.
    private static final int BUTTON_BAND_DIVIDER_GAP_ABOVE = TITLE_RULE_GAP_ABOVE;
    private static final int BUTTON_BAND_DIVIDER_HEIGHT = TITLE_RULE_HEIGHT;
    private static final int BUTTON_BAND_VERTICAL_MARGIN = 6;
    // Horizontal gap between two buttons that land in the same left/center/right group.
    private static final int BUTTON_GAP = 6;

    // Close-button band: a footer strip reserved exclusively for the close button (see
    // PanelGeometry's javadoc). It uses the same divider visual language as the title rule, then a
    // margin above and below the button itself. Fixed size, independent of content, so it is
    // never squeezed by a tall content file — PanelGeometry.of() always adds this on top of the
    // measured text-area height (and the content-button band) before clamping the panel's total
    // height.
    private static final int CLOSE_BAND_DIVIDER_GAP_ABOVE = TITLE_RULE_GAP_ABOVE;
    private static final int CLOSE_BAND_DIVIDER_HEIGHT = TITLE_RULE_HEIGHT;
    private static final int CLOSE_BUTTON_MARGIN = 6;
    private static final int CLOSE_BAND_HEIGHT =
            CLOSE_BAND_DIVIDER_GAP_ABOVE + CLOSE_BAND_DIVIDER_HEIGHT + CLOSE_BUTTON_MARGIN * 2 + BUTTON_HEIGHT;

    private static final int PANEL_BACKGROUND_COLOR = 0xE0101010;
    private static final int PANEL_BORDER_COLOR = 0x80FFFFFF;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int BODY_COLOR = 0xFFCCCCCC;
    private static final int TITLE_RULE_COLOR = 0x33FFFFFF;

    private final WelcomeContent content;
    private final Runnable onClosed;

    private PanelGeometry panel;

    public WelcomeBoardScreen(WelcomeContent content, Runnable onClosed) {
        super(Component.literal(content.title()));
        this.content = content;
        this.onClosed = onClosed;
    }

    @Override
    protected void init() {
        // Panel width only depends on screen width (PanelGeometry#widthFor), so it can be resolved
        // before the panel itself: word-wrapping the body (needed to measure how tall the content
        // actually is) needs that width first.
        int panelWidth = PanelGeometry.widthFor(width);
        int textWidth = computeTextWidth(panelWidth);
        int desiredTextAreaHeight = estimateTextAreaHeight(textWidth);
        int buttonBandHeight = PanelGeometry.buttonBandHeightFor(content.buttons().size(), BUTTON_HEIGHT,
                BUTTON_BAND_VERTICAL_MARGIN, BUTTON_BAND_DIVIDER_GAP_ABOVE, BUTTON_BAND_DIVIDER_HEIGHT);

        this.panel = PanelGeometry.of(width, height, panelWidth, desiredTextAreaHeight, buttonBandHeight,
                CLOSE_BAND_HEIGHT);
        if (panel.textAreaHeight() < desiredTextAreaHeight) {
            Constants.LOG.warn(
                    "Welcome Board: content needs about {}px of height but the panel's text area is capped at {}px on a {}x{} screen; truncating the overflow.",
                    desiredTextAreaHeight, panel.textAreaHeight(), width, height);
        }

        addCloseButton();
        layoutButtonBand();
    }

    private boolean hasImage() {
        WelcomeImage image = content.image();
        return image != null && image.width() > 0 && image.height() > 0;
    }

    /** Mirrors the text-area width {@link #renderPanelContents} actually draws into. */
    private int computeTextWidth(int panelWidth) {
        return panelWidth - PADDING * 2;
    }

    /**
     * Sums the pixel height the text area alone (title + body + image — never the content-button
     * band or the close-button band, both fixed-size and added separately by {@link
     * PanelGeometry#of}) will actually occupy at {@code textWidth}, so {@link PanelGeometry} can
     * size the panel to match instead of guessing.
     */
    private int estimateTextAreaHeight(int textWidth) {
        int contentHeight = PADDING;

        if (!content.title().isEmpty()) {
            contentHeight += font.lineHeight + TITLE_RULE_GAP_ABOVE + TITLE_RULE_HEIGHT + TITLE_RULE_GAP_BELOW;
        }

        for (String bodyLine : content.body()) {
            List<FormattedCharSequence> wrapped = font.split(FormattedText.of(bodyLine), textWidth);
            contentHeight += wrapped.size() * (font.lineHeight + LINE_SPACING);
        }

        if (hasImage()) {
            contentHeight += content.image().height();
        }

        return contentHeight + PADDING;
    }

    private void addCloseButton() {
        String closeLabelText = content.closeText().isEmpty() ? "Close" : content.closeText();
        Component closeLabel = Component.literal(closeLabelText);
        int closeWidth = Math.max(60, font.width(closeLabel) + 20);

        // Centered, alone, in the close-button band — the panel's one structurally-guaranteed
        // primary action (never shares the row with a pack-author button, unlike the layout this
        // replaced).
        int closeX = panel.x() + (panel.width() - closeWidth) / 2;
        int closeY = panel.y() + panel.height() - CLOSE_BUTTON_MARGIN - BUTTON_HEIGHT;

        addRenderableWidget(Button.builder(closeLabel, button -> onClose())
                .bounds(closeX, closeY, closeWidth, BUTTON_HEIGHT)
                .build());
    }

    /**
     * Lays out every pack-author button ({@code content.buttons()}) inside the content-button
     * band exclusively (never the text area, never the close-button band — see {@link
     * PanelGeometry}). Buttons are grouped by the horizontal component of their anchor (see
     * {@link #horizontalAlignOf}) and packed onto the band's one row: left-anchored buttons flow
     * left-to-right from the band's left edge, right-anchored buttons flow right-to-left from the
     * band's right edge, and center-anchored buttons are centered as one contiguous block.
     *
     * <p>If the declared buttons don't all fit on the one row at once, the ones that don't fit are
     * dropped (in declaration order, starting from the last button in the content file) rather
     * than wrapped onto a second row — the band is a fixed single-row strip (see {@link
     * PanelGeometry#buttonBandHeightFor}) — and a warning is logged so the pack author can shorten
     * labels or remove buttons.
     */
    private void layoutButtonBand() {
        List<WelcomeButton> allButtons = content.buttons();
        if (allButtons.isEmpty() || panel.buttonBandHeight() <= 0) {
            return;
        }

        int bandX = panel.x() + PADDING;
        int bandWidth = Math.max(0, panel.width() - PADDING * 2);
        int buttonY = panel.buttonBandY() + BUTTON_BAND_DIVIDER_GAP_ABOVE + BUTTON_BAND_DIVIDER_HEIGHT
                + BUTTON_BAND_VERTICAL_MARGIN;

        List<WelcomeButton> laidOut = new ArrayList<>(allButtons);
        while (!laidOut.isEmpty() && packedWidth(laidOut) > bandWidth) {
            laidOut.remove(laidOut.size() - 1);
        }
        if (laidOut.size() < allButtons.size()) {
            Constants.LOG.warn(
                    "Welcome Board: {} of {} content button(s) don't fit in the {}px-wide button band and were dropped (in declaration order); use fewer buttons or shorter labels.",
                    allButtons.size() - laidOut.size(), allButtons.size(), bandWidth);
        }

        List<WelcomeButton> left = new ArrayList<>();
        List<WelcomeButton> center = new ArrayList<>();
        List<WelcomeButton> right = new ArrayList<>();
        for (WelcomeButton button : laidOut) {
            switch (horizontalAlignOf(button.anchor())) {
                case LEFT -> left.add(button);
                case CENTER -> center.add(button);
                case RIGHT -> right.add(button);
            }
        }

        int leftWidth = packedWidth(left);
        int centerWidth = packedWidth(center);
        int rightWidth = packedWidth(right);

        int x = bandX;
        for (WelcomeButton button : left) {
            int buttonWidth = buttonWidth(button);
            addButtonWidgetAt(button, x, buttonY, buttonWidth);
            x += buttonWidth + BUTTON_GAP;
        }

        // Not a plain "center in the whole band" — that would ignore the space the left/right
        // zones already occupy and can overlap them even though the total width fits (see
        // PanelGeometry#centerBlockStart's javadoc for the concrete case).
        x = bandX + PanelGeometry.centerBlockStart(bandWidth, leftWidth, centerWidth, rightWidth, BUTTON_GAP);
        for (WelcomeButton button : center) {
            int buttonWidth = buttonWidth(button);
            addButtonWidgetAt(button, x, buttonY, buttonWidth);
            x += buttonWidth + BUTTON_GAP;
        }

        x = bandX + bandWidth;
        for (WelcomeButton button : right) {
            int buttonWidth = buttonWidth(button);
            x -= buttonWidth;
            addButtonWidgetAt(button, x, buttonY, buttonWidth);
            x -= BUTTON_GAP;
        }
    }

    /**
     * Maps a content-file anchor to the button band's horizontal placement only. Buttons always
     * live in the band's one fixed row (see {@link PanelGeometry}), so only the anchor's
     * left/center/right component is meaningful for a button; any vertical component ({@code
     * TOP_}/{@code BOTTOM_}/bare) is ignored here. That vertical component still matters for
     * images (and implicitly title/body), which resolve against the text area instead — see
     * {@link #resolveInPanel}.
     *
     * <pre>
     * Anchor value                        -&gt; button horizontal placement
     * TOP_LEFT, LEFT, BOTTOM_LEFT          -&gt; left-aligned
     * TOP, CENTER, BOTTOM                  -&gt; centered
     * TOP_RIGHT, RIGHT, BOTTOM_RIGHT       -&gt; right-aligned
     * </pre>
     */
    private static HorizontalAlign horizontalAlignOf(Anchor anchor) {
        Anchor effective = anchor != null ? anchor : Anchor.DEFAULT;
        return switch (effective) {
            case TOP_LEFT, LEFT, BOTTOM_LEFT -> HorizontalAlign.LEFT;
            case TOP, CENTER, BOTTOM -> HorizontalAlign.CENTER;
            case TOP_RIGHT, RIGHT, BOTTOM_RIGHT -> HorizontalAlign.RIGHT;
        };
    }

    private enum HorizontalAlign {
        LEFT, CENTER, RIGHT
    }

    /** Total width a row of buttons occupies, including the {@link #BUTTON_GAP} between them. */
    private int packedWidth(List<WelcomeButton> buttons) {
        int total = 0;
        for (int i = 0; i < buttons.size(); i++) {
            total += buttonWidth(buttons.get(i));
            if (i > 0) {
                total += BUTTON_GAP;
            }
        }
        return total;
    }

    private int buttonWidth(WelcomeButton welcomeButton) {
        return Math.max(80, font.width(buttonLabel(welcomeButton)) + 20);
    }

    private Component buttonLabel(WelcomeButton welcomeButton) {
        String labelText = welcomeButton.text().isEmpty() ? welcomeButton.url() : welcomeButton.text();
        return Component.literal(labelText);
    }

    private void addButtonWidgetAt(WelcomeButton welcomeButton, int x, int y, int buttonWidth) {
        addRenderableWidget(Button.builder(buttonLabel(welcomeButton), button -> openConfirmLink(welcomeButton.url()))
                .bounds(x, y, buttonWidth, BUTTON_HEIGHT)
                .build());
    }

    /**
     * Resolves a content-file anchor against the panel's <em>text area</em> — the top strip above
     * both the content-button band and the close-button band, per {@link PanelGeometry} — never
     * against the full panel and never against the full screen. Used for the image only (title and
     * body render directly, without going through an anchor); pack-author buttons are laid out
     * separately by {@link #layoutButtonBand} against the content-button band instead, so a
     * content-file anchor on a button can never resolve into this rectangle. {@link
     * LayoutResolver} itself is unchanged (still a screen-size-agnostic pure function); only the
     * rectangle this caller feeds it changed — and that rectangle is inset by {@link #PADDING} on
     * every side (matching the text's own inset), so a corner anchor lands with the same breathing
     * room as the text area's edge instead of flush against the border.
     */
    private Point resolveInPanel(Anchor anchor, int elementWidth, int elementHeight, int offsetX, int offsetY) {
        int innerWidth = Math.max(0, panel.width() - PADDING * 2);
        int innerHeight = Math.max(0, panel.textAreaHeight() - PADDING * 2);
        Point resolved = LayoutResolver.resolve(innerWidth, innerHeight, elementWidth, elementHeight,
                anchor, offsetX, offsetY);
        return new Point(panel.x() + PADDING + resolved.x(), panel.y() + PADDING + resolved.y());
    }

    private void openConfirmLink(String url) {
        minecraft.gui.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                net.minecraft.util.Util.getPlatform().openUri(url);
            }
            minecraft.gui.setScreen(this);
        }, url, true));
    }

    /**
     * Panel chrome only — everything that must sit behind the close/content buttons. The stratum
     * boundary vanilla inserts between this pass and {@link #extractRenderState} is what actually
     * guarantees that depth ordering, so nothing here may be moved into the later pass.
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        extractor.fill(panel.x(), panel.y(), panel.x() + panel.width(), panel.y() + panel.height(), PANEL_BACKGROUND_COLOR);
        extractor.outline(panel.x(), panel.y(), panel.width(), panel.height(), PANEL_BORDER_COLOR);
        renderTitleRule(extractor);
        renderButtonBandDivider(extractor);
        renderCloseBandDivider(extractor);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        renderPanelContents(extractor);
        renderImage(extractor);
    }

    /** Y coordinate of the title's divider rule, or -1 when the content file declares no title. */
    private int titleRuleY() {
        if (content.title().isEmpty()) {
            return -1;
        }
        return panel.y() + PADDING + font.lineHeight + TITLE_RULE_GAP_ABOVE;
    }

    /**
     * Divider under the title. Bold weight alone left the title relying on the barely-perceptible
     * white-vs-light-gray color difference against a dark panel; the rule gives it a real break
     * from the body (negative_list.md: no glow/gradient/dot — just weight + a muted rule, same
     * register as vanilla's own panel headers).
     */
    private void renderTitleRule(GuiGraphicsExtractor extractor) {
        int y = titleRuleY();
        if (y < 0) {
            return;
        }
        int textX = panel.x() + PADDING;
        int textWidth = computeTextWidth(panel.width());
        extractor.fill(textX, y, textX + textWidth, y + TITLE_RULE_HEIGHT, TITLE_RULE_COLOR);
    }

    /**
     * Left-aligned title/body block, clamped to the text area (never past its bottom edge — that
     * space belongs to the content-button band and the close-button band, see {@link
     * PanelGeometry}). The title's divider rule is chrome and lives in {@link #renderTitleRule}
     * instead; this method only advances past the space that rule occupies.
     */
    private void renderPanelContents(GuiGraphicsExtractor extractor) {
        int textX = panel.x() + PADDING;
        int textWidth = computeTextWidth(panel.width());
        int maxY = panel.buttonBandY();

        int y = panel.y() + PADDING;

        if (!content.title().isEmpty()) {
            Component title = Component.literal(content.title()).withStyle(ChatFormatting.BOLD);
            extractor.text(font, title, textX, y, TITLE_COLOR);
            y += font.lineHeight + TITLE_RULE_GAP_ABOVE + TITLE_RULE_HEIGHT + TITLE_RULE_GAP_BELOW;
        }

        for (String bodyLine : content.body()) {
            List<FormattedCharSequence> wrappedLines = font.split(FormattedText.of(bodyLine), textWidth);
            for (FormattedCharSequence wrapped : wrappedLines) {
                if (y + font.lineHeight > maxY) {
                    return; // Clamp rather than overflow into the content-button/close-button bands.
                }
                extractor.text(font, wrapped, textX, y, BODY_COLOR);
                y += font.lineHeight + LINE_SPACING;
            }
        }
    }

    /**
     * Divider between the text area and the content-button band, same register as the title rule.
     * Only drawn when the band actually exists (a content file with no buttons has nothing to
     * divide from).
     */
    private void renderButtonBandDivider(GuiGraphicsExtractor extractor) {
        if (panel.buttonBandHeight() <= 0) {
            return;
        }
        int textX = panel.x() + PADDING;
        int textWidth = computeTextWidth(panel.width());
        int y = panel.buttonBandY() + BUTTON_BAND_DIVIDER_GAP_ABOVE;
        extractor.fill(textX, y, textX + textWidth, y + BUTTON_BAND_DIVIDER_HEIGHT, TITLE_RULE_COLOR);
    }

    /** Divider between the content-button band and the close-button band, same register as the title rule. */
    private void renderCloseBandDivider(GuiGraphicsExtractor extractor) {
        int textX = panel.x() + PADDING;
        int textWidth = computeTextWidth(panel.width());
        int y = panel.closeBandY() + CLOSE_BAND_DIVIDER_GAP_ABOVE;
        extractor.fill(textX, y, textX + textWidth, y + CLOSE_BAND_DIVIDER_HEIGHT, TITLE_RULE_COLOR);
    }

    private void renderImage(GuiGraphicsExtractor extractor) {
        if (!hasImage()) {
            return;
        }
        WelcomeImage image = content.image();
        Identifier texture = Identifier.tryParse(image.id());
        if (texture == null) {
            return;
        }
        Point position = resolveInPanel(image.anchor(), image.width(), image.height(),
                image.offsetX(), image.offsetY());
        extractor.blit(RenderPipelines.GUI_TEXTURED, texture, position.x(), position.y(), 0.0F, 0.0F,
                image.width(), image.height(), image.width(), image.height());
    }

    @Override
    public void onClose() {
        // Deliberately does not call super.onClose(): the caller (DisplayQueueController) owns
        // what screen comes next — either null, or the next queued welcome-board screen — and
        // sets it explicitly. Calling super here would race that decision back to null.
        if (onClosed != null) {
            onClosed.run();
        } else {
            minecraft.gui.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
