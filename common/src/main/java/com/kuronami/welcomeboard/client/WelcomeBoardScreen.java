package com.kuronami.welcomeboard.client;

import com.kuronami.welcomeboard.config.LayoutPreset;
import com.kuronami.welcomeboard.content.WelcomeButton;
import com.kuronami.welcomeboard.content.WelcomeContent;
import com.kuronami.welcomeboard.content.WelcomeImage;
import com.kuronami.welcomeboard.layout.LayoutResolver;
import com.kuronami.welcomeboard.layout.Point;

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
 * button are placed at their own content-file anchor via {@link LayoutResolver} (screen-relative,
 * pack-author controlled); title/body/close are placed by {@link PanelGeometry} + the
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

    private static final int PANEL_BACKGROUND_COLOR = 0xE0101010;
    private static final int PANEL_BORDER_COLOR = 0x80FFFFFF;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int BODY_COLOR = 0xFFCCCCCC;

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
        this.panel = PanelGeometry.forPreset(preset, width, height);
        addCloseButton();
        for (WelcomeButton welcomeButton : content.buttons()) {
            addButtonWidget(welcomeButton);
        }
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
        Point position = LayoutResolver.resolve(width, height, buttonWidth, BUTTON_HEIGHT,
                welcomeButton.anchor(), welcomeButton.offsetX(), welcomeButton.offsetY());

        addRenderableWidget(Button.builder(label, button -> openConfirmLink(welcomeButton.url()))
                .bounds(position.x(), position.y(), buttonWidth, BUTTON_HEIGHT)
                .build());
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
        boolean hasImage = content.image() != null && content.image().width() > 0 && content.image().height() > 0;
        boolean bannerImageColumn = preset == LayoutPreset.BANNER && hasImage;

        int textX = panel.x() + PADDING + (bannerImageColumn ? BANNER_IMAGE_COLUMN_WIDTH : 0);
        int textWidth = panel.width() - PADDING * 2 - (bannerImageColumn ? BANNER_IMAGE_COLUMN_WIDTH : 0)
                - (preset == LayoutPreset.BANNER ? PADDING * 2 : 0);
        int maxY = preset == LayoutPreset.BANNER
                ? panel.y() + panel.height() - PADDING
                : panel.y() + panel.height() - BUTTON_HEIGHT - CLOSE_BUTTON_MARGIN - PADDING / 2;

        int y = panel.y() + PADDING;
        boolean centered = preset == LayoutPreset.FULL;
        int centerX = panel.x() + panel.width() / 2;

        if (!content.title().isEmpty()) {
            Component title = Component.literal(content.title());
            if (centered) {
                guiGraphics.drawCenteredString(font, title, centerX, y, TITLE_COLOR);
            } else {
                guiGraphics.drawString(font, title, textX, y, TITLE_COLOR);
            }
            y += font.lineHeight + LINE_SPACING * 2;
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
        WelcomeImage image = content.image();
        if (image == null || image.width() <= 0 || image.height() <= 0) {
            return;
        }
        ResourceLocation texture = ResourceLocation.tryParse(image.id());
        if (texture == null) {
            return;
        }
        Point position = LayoutResolver.resolve(width, height, image.width(), image.height(),
                image.anchor(), image.offsetX(), image.offsetY());
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
