package com.kuronami.welcomeboard.client;

import com.kuronami.welcomeboard.Constants;
import com.kuronami.welcomeboard.config.LayoutPreset;
import com.kuronami.welcomeboard.content.Anchor;
import com.kuronami.welcomeboard.content.WelcomeButton;
import com.kuronami.welcomeboard.content.WelcomeContent;
import com.kuronami.welcomeboard.content.WelcomeImage;
import com.kuronami.welcomeboard.layout.LayoutResolver;
import com.kuronami.welcomeboard.layout.Point;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Draws one welcome-board content file (DESIGN_COMPILE.md U4). The optional image and each
 * button are placed at their own content-file anchor via {@link LayoutResolver} (panel-relative,
 * pack-author controlled — the anchor is resolved against {@link #panel}'s rectangle, not the
 * full screen); title/body/close are placed by {@link PanelGeometry} + the
 * {@link LayoutPreset}-specific arrangement below — see {@link #renderPanelContents} for why the
 * three presets are not just "the same dialog at a different size" (DESIGN_COMPILE.md §8's
 * candidate-sheet gate needs them to actually read as different layouts side by side).
 *
 * <p>URL buttons never open a link directly (DESIGN_COMPILE.md's constraint): they push vanilla's
 * {@link ConfirmLinkScreen} first, same as every other in-game "open this link?" prompt, and
 * return to this screen afterward regardless of the player's answer.
 *
 * <p>This class lives in {@code common}'s {@code client} package rather than being duplicated per
 * loader — the existing {@code mixin.MixinMinecraft}/{@code MixinTitleScreen} in this same module
 * already reference client-only Minecraft types ({@code Minecraft}, {@code TitleScreen}) from
 * common, and other multiloader mods in this project (e.g. mod-051's {@code
 * client.MusicDiscMakerScreen}) follow the same convention: the class is never referenced from a
 * code path that also runs on a dedicated server, so its presence in the common jar does not
 * force it to be loaded there.
 */
public final class WelcomeBoardScreen extends Screen {

    private static final int PADDING = 10;
    private static final int LINE_SPACING = 1;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CLOSE_BUTTON_MARGIN = 6;
    private static final int BANNER_IMAGE_COLUMN_WIDTH = 48;

    // Gap above/below the title-divider rule (defect ③: title vs body needed a real break, not
    // just a color difference nobody notices against a dark panel — see #renderPanelContents).
    private static final int TITLE_RULE_GAP_ABOVE = 3;
    private static final int TITLE_RULE_GAP_BELOW = 4;
    private static final int TITLE_RULE_HEIGHT = 1;

    private static final int PANEL_BACKGROUND_COLOR = 0xE0101010;
    private static final int PANEL_BORDER_COLOR = 0x80FFFFFF;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int BODY_COLOR = 0xFFCCCCCC;
    private static final int TITLE_RULE_COLOR = 0x33FFFFFF;

    private final WelcomeContent content;
    private final LayoutPreset preset;
    private final Runnable onClosed;

    private PanelGeometry panel;

    public WelcomeBoardScreen(WelcomeContent content, LayoutPreset preset, Runnable onClosed) {
        super(Component.literal(content.title()));
        this.content = content;
        this.preset = preset != null ? preset : LayoutPreset.DEFAULT;
        this.onClosed = onClosed;
    }

    @Override
    protected void init() {
        // Panel width only depends on the preset + screen width (PanelGeometry#widthFor), so it
        // can be resolved before the panel itself: word-wrapping the body (needed to measure how
        // tall the content actually is) needs that width first.
        int panelWidth = PanelGeometry.widthFor(preset, width);
        boolean bannerImageColumn = preset == LayoutPreset.BANNER && hasImage();
        int textWidth = computeTextWidth(panelWidth, bannerImageColumn);
        int desiredContentHeight = estimateContentHeight(textWidth, bannerImageColumn);

        this.panel = PanelGeometry.forPreset(preset, width, height, desiredContentHeight);
        if (panel.height() < desiredContentHeight) {
            Constants.LOG.warn(
                    "Welcome Board: content needs about {}px of height but the {} panel is capped at {}px on a {}x{} screen; truncating the overflow.",
                    desiredContentHeight, preset, panel.height(), width, height);
        }

        addCloseButton();
        for (WelcomeButton welcomeButton : content.buttons()) {
            addButtonWidget(welcomeButton);
        }
    }

    private boolean hasImage() {
        WelcomeImage image = content.image();
        return image != null && image.width() > 0 && image.height() > 0;
    }

    /** Mirrors the text-area width {@link #renderPanelContents} actually draws into. */
    private int computeTextWidth(int panelWidth, boolean bannerImageColumn) {
        return panelWidth - PADDING * 2 - (bannerImageColumn ? BANNER_IMAGE_COLUMN_WIDTH : 0)
                - (preset == LayoutPreset.BANNER ? PADDING * 2 : 0);
    }

    /**
     * Defect ②: the panel used to be a fixed fraction of the screen regardless of content, leaving
     * a dead gap between the body and the close button whenever a file had little to say. This sums
     * the pixel height the content will actually occupy at {@code textWidth} — title + divider rule,
     * every wrapped body line, the image (stacked into the flow for CARD/FULL; just a floor for
     * BANNER's side-by-side image column), and the close-button row — so {@link PanelGeometry} can
     * size the panel to match instead of guessing.
     */
    private int estimateContentHeight(int textWidth, boolean bannerImageColumn) {
        int contentHeight = PADDING;

        if (!content.title().isEmpty()) {
            contentHeight += font.lineHeight + TITLE_RULE_GAP_ABOVE + TITLE_RULE_HEIGHT + TITLE_RULE_GAP_BELOW;
        }

        for (String bodyLine : content.body()) {
            List<FormattedCharSequence> wrapped = font.split(FormattedText.of(bodyLine), textWidth);
            contentHeight += wrapped.size() * (font.lineHeight + LINE_SPACING);
        }

        if (hasImage()) {
            int imageHeight = content.image().height();
            if (bannerImageColumn) {
                // Side-by-side column, not stacked below the text: the panel just needs to be at
                // least as tall as the image, not taller by the image's height on top of the text.
                contentHeight = Math.max(contentHeight, PADDING + imageHeight);
            } else {
                contentHeight += imageHeight;
            }
        }

        if (preset != LayoutPreset.BANNER) {
            // BANNER's close button is vertically centered in the panel (see #addCloseButton), not
            // a reserved bottom row, so it doesn't add to the content's own vertical footprint.
            contentHeight += CLOSE_BUTTON_MARGIN + BUTTON_HEIGHT + PADDING / 2;
        }

        return contentHeight + PADDING;
    }

    private void addCloseButton() {
        String closeLabelText = content.closeText().isEmpty() ? "Close" : content.closeText();
        Component closeLabel = Component.literal(closeLabelText);
        int closeWidth = Math.max(60, font.width(closeLabel) + 20);

        int closeX;
        int closeY;
        if (preset == LayoutPreset.BANNER) {
            // Toast-like dismiss: pinned to the panel's right edge instead of bottom-center, so
            // BANNER actually reads as a different arrangement rather than a shorter CARD.
            closeX = panel.x() + panel.width() - closeWidth - PADDING;
            closeY = panel.y() + (panel.height() - BUTTON_HEIGHT) / 2;
        } else {
            closeX = panel.x() + (panel.width() - closeWidth) / 2;
            closeY = panel.y() + panel.height() - BUTTON_HEIGHT - CLOSE_BUTTON_MARGIN;
        }

        addRenderableWidget(Button.builder(closeLabel, button -> onClose())
                .bounds(closeX, closeY, closeWidth, BUTTON_HEIGHT)
                .build());
    }

    private void addButtonWidget(WelcomeButton welcomeButton) {
        String labelText = welcomeButton.text().isEmpty() ? welcomeButton.url() : welcomeButton.text();
        Component label = Component.literal(labelText);
        int buttonWidth = Math.max(80, font.width(label) + 20);
        Point position = resolveInPanel(welcomeButton.anchor(), buttonWidth, BUTTON_HEIGHT,
                welcomeButton.offsetX(), welcomeButton.offsetY());

        addRenderableWidget(Button.builder(label, button -> openConfirmLink(welcomeButton.url()))
                .bounds(position.x(), position.y(), buttonWidth, BUTTON_HEIGHT)
                .build());
    }

    /**
     * Defect ①: anchors are resolved against the panel rectangle, not the full screen — a
     * content-file "bottom_left" button/image belongs in the panel's bottom-left corner, not stuck
     * to the screen's corner where it reads as unrelated to the dialog and collides with the
     * hotbar. {@link LayoutResolver} itself is unchanged (still a screen-size-agnostic pure
     * function); only the rectangle callers feed it changed — and that rectangle is inset by
     * {@link #PADDING} on every side (matching the text's own inset), so a corner anchor lands
     * with the same breathing room as the panel edge instead of flush against the border.
     */
    private Point resolveInPanel(Anchor anchor, int elementWidth, int elementHeight, int offsetX, int offsetY) {
        int innerWidth = Math.max(0, panel.width() - PADDING * 2);
        int innerHeight = Math.max(0, panel.height() - PADDING * 2);
        Point resolved = LayoutResolver.resolve(innerWidth, innerHeight, elementWidth, elementHeight,
                anchor, offsetX, offsetY);
        return new Point(panel.x() + PADDING + resolved.x(), panel.y() + PADDING + resolved.y());
    }

    private void openConfirmLink(String url) {
        minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                net.minecraft.Util.getPlatform().openUri(url);
            }
            minecraft.setScreen(this);
        }, url, true));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw the panel here, not in render(): Screen#render (called via super.render below)
        // draws renderBackground first and then every widget on top of it, so filling the panel
        // from render() after super.render() would paint over the close/content buttons.
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(panel.x(), panel.y(), panel.x() + panel.width(), panel.y() + panel.height(), PANEL_BACKGROUND_COLOR);
        guiGraphics.renderOutline(panel.x(), panel.y(), panel.width(), panel.height(), PANEL_BORDER_COLOR);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderPanelContents(guiGraphics);
        renderImage(guiGraphics);
    }

    /**
     * Title/body placement genuinely differs per preset, not just the panel's size — the whole
     * point of the three-way comparison (DESIGN_COMPILE.md §8):
     * <ul>
     *   <li>{@code CARD}: classic left-aligned dialog text block.</li>
     *   <li>{@code BANNER}: a left text column that leaves room for the image column and the
     *       right-pinned close button (see {@link #addCloseButton}), not a shrunk CARD.</li>
     *   <li>{@code FULL}: centered title and centered body lines, filling the larger panel.</li>
     * </ul>
     * Body text is clamped to the space above the close button (or, for {@code BANNER}, above the
     * panel's bottom edge) rather than left to overflow past it.
     */
    private void renderPanelContents(GuiGraphics guiGraphics) {
        boolean bannerImageColumn = preset == LayoutPreset.BANNER && hasImage();

        int textX = panel.x() + PADDING + (bannerImageColumn ? BANNER_IMAGE_COLUMN_WIDTH : 0);
        int textWidth = computeTextWidth(panel.width(), bannerImageColumn);
        int maxY = preset == LayoutPreset.BANNER
                ? panel.y() + panel.height() - PADDING
                : panel.y() + panel.height() - BUTTON_HEIGHT - CLOSE_BUTTON_MARGIN - PADDING / 2;

        int y = panel.y() + PADDING;
        boolean centered = preset == LayoutPreset.FULL;
        int centerX = panel.x() + panel.width() / 2;

        if (!content.title().isEmpty()) {
            // Defect ③: bold weight + a divider rule gives the title a real break from the body
            // instead of relying on the barely-perceptible white-vs-light-gray color difference
            // alone (negative_list.md: no glow/gradient/dot — just weight + a muted rule, same
            // register as vanilla's own panel headers).
            Component title = Component.literal(content.title()).withStyle(ChatFormatting.BOLD);
            if (centered) {
                guiGraphics.drawCenteredString(font, title, centerX, y, TITLE_COLOR);
            } else {
                guiGraphics.drawString(font, title, textX, y, TITLE_COLOR);
            }
            y += font.lineHeight + TITLE_RULE_GAP_ABOVE;
            guiGraphics.fill(textX, y, textX + textWidth, y + TITLE_RULE_HEIGHT, TITLE_RULE_COLOR);
            y += TITLE_RULE_HEIGHT + TITLE_RULE_GAP_BELOW;
        }

        for (String bodyLine : content.body()) {
            List<FormattedCharSequence> wrappedLines = font.split(FormattedText.of(bodyLine), textWidth);
            for (FormattedCharSequence wrapped : wrappedLines) {
                if (y + font.lineHeight > maxY) {
                    return; // Clamp rather than overflow onto the close button/panel edge.
                }
                if (centered) {
                    guiGraphics.drawCenteredString(font, wrapped, centerX, y, BODY_COLOR);
                } else {
                    guiGraphics.drawString(font, wrapped, textX, y, BODY_COLOR);
                }
                y += font.lineHeight + LINE_SPACING;
            }
        }
    }

    private void renderImage(GuiGraphics guiGraphics) {
        if (!hasImage()) {
            return;
        }
        WelcomeImage image = content.image();
        ResourceLocation texture = ResourceLocation.tryParse(image.id());
        if (texture == null) {
            return;
        }
        Point position = resolveInPanel(image.anchor(), image.width(), image.height(),
                image.offsetX(), image.offsetY());
        guiGraphics.blit(texture, position.x(), position.y(), 0.0F, 0.0F,
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
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
