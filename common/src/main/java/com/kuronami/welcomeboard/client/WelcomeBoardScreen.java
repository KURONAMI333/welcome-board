package com.kuronami.welcomeboard.client;

import com.kuronami.welcomeboard.Constants;
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
 * Draws one welcome-board content file (DESIGN_COMPILE.md U4): a single centered modal card,
 * structurally split into a content area (title/body/image/pack-author buttons — see {@link
 * #renderPanelContents}) and a dedicated close-button band at the panel's bottom edge (see
 * {@link PanelGeometry}). The content area and the close-button band never share space: the
 * content area's height excludes the band entirely, and every content-file anchor (buttons,
 * image) resolves against the content area's rectangle, not the full panel — so a pack author's
 * {@code bottom_left}/{@code bottom_right}/{@code center} placement can never land the close
 * button underneath or beside their own button (see {@link #resolveInPanel}). It remains possible
 * for a pack author's own body text and their own button to overlap each other (e.g. a button
 * anchored {@code center}); that is the author's own placement choice, not something this class
 * guards against.
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

    // Gap above/below the title-divider rule (title needed a real break from the body, not just a
    // color difference nobody notices against a dark panel — see #renderPanelContents).
    private static final int TITLE_RULE_GAP_ABOVE = 3;
    private static final int TITLE_RULE_GAP_BELOW = 4;
    private static final int TITLE_RULE_HEIGHT = 1;

    // Close-button band: a footer strip reserved exclusively for the close button (see
    // PanelGeometry's javadoc). It uses the same divider visual language as the title rule, then a
    // margin above and below the button itself. Fixed size, independent of content, so it is
    // never squeezed by a tall content file — PanelGeometry.of() always adds this on top of the
    // measured content-area height before clamping the panel's total height.
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
        int desiredContentAreaHeight = estimateContentAreaHeight(textWidth);

        this.panel = PanelGeometry.of(width, height, panelWidth, desiredContentAreaHeight, CLOSE_BAND_HEIGHT);
        if (panel.contentAreaHeight() < desiredContentAreaHeight) {
            Constants.LOG.warn(
                    "Welcome Board: content needs about {}px of height but the panel's content area is capped at {}px on a {}x{} screen; truncating the overflow.",
                    desiredContentAreaHeight, panel.contentAreaHeight(), width, height);
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
    private int computeTextWidth(int panelWidth) {
        return panelWidth - PADDING * 2;
    }

    /**
     * Sums the pixel height the content area alone (title + body + image — never the close-button
     * band, which is fixed-size and added separately by {@link PanelGeometry#of}) will actually
     * occupy at {@code textWidth}, so {@link PanelGeometry} can size the panel to match instead of
     * guessing.
     */
    private int estimateContentAreaHeight(int textWidth) {
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
     * Resolves a content-file anchor against the panel's <em>content area</em> — the strip above
     * the close-button band, per {@link PanelGeometry} — not the full panel and not the full
     * screen. A content-file "bottom_left" button/image belongs in the content area's bottom-left
     * corner, which structurally sits above the close button rather than colliding with it.
     * {@link LayoutResolver} itself is unchanged (still a screen-size-agnostic pure function);
     * only the rectangle callers feed it changed — and that rectangle is inset by {@link
     * #PADDING} on every side (matching the text's own inset), so a corner anchor lands with the
     * same breathing room as the content area's edge instead of flush against the border.
     */
    private Point resolveInPanel(Anchor anchor, int elementWidth, int elementHeight, int offsetX, int offsetY) {
        int innerWidth = Math.max(0, panel.width() - PADDING * 2);
        int innerHeight = Math.max(0, panel.contentAreaHeight() - PADDING * 2);
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
        renderCloseBandDivider(guiGraphics);
        renderImage(guiGraphics);
    }

    /**
     * Left-aligned title/body block, clamped to the content area (never past its bottom edge —
     * that space belongs to the close-button band, see {@link PanelGeometry}).
     */
    private void renderPanelContents(GuiGraphics guiGraphics) {
        int textX = panel.x() + PADDING;
        int textWidth = computeTextWidth(panel.width());
        int maxY = panel.closeBandY();

        int y = panel.y() + PADDING;

        if (!content.title().isEmpty()) {
            // Bold weight + a divider rule gives the title a real break from the body instead of
            // relying on the barely-perceptible white-vs-light-gray color difference alone
            // (negative_list.md: no glow/gradient/dot — just weight + a muted rule, same register
            // as vanilla's own panel headers).
            Component title = Component.literal(content.title()).withStyle(ChatFormatting.BOLD);
            guiGraphics.drawString(font, title, textX, y, TITLE_COLOR);
            y += font.lineHeight + TITLE_RULE_GAP_ABOVE;
            guiGraphics.fill(textX, y, textX + textWidth, y + TITLE_RULE_HEIGHT, TITLE_RULE_COLOR);
            y += TITLE_RULE_HEIGHT + TITLE_RULE_GAP_BELOW;
        }

        for (String bodyLine : content.body()) {
            List<FormattedCharSequence> wrappedLines = font.split(FormattedText.of(bodyLine), textWidth);
            for (FormattedCharSequence wrapped : wrappedLines) {
                if (y + font.lineHeight > maxY) {
                    return; // Clamp rather than overflow into the close-button band.
                }
                guiGraphics.drawString(font, wrapped, textX, y, BODY_COLOR);
                y += font.lineHeight + LINE_SPACING;
            }
        }
    }

    /** Divider between the content area and the close-button band, same register as the title rule. */
    private void renderCloseBandDivider(GuiGraphics guiGraphics) {
        int textX = panel.x() + PADDING;
        int textWidth = computeTextWidth(panel.width());
        int y = panel.closeBandY() + CLOSE_BAND_DIVIDER_GAP_ABOVE;
        guiGraphics.fill(textX, y, textX + textWidth, y + CLOSE_BAND_DIVIDER_HEIGHT, TITLE_RULE_COLOR);
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
