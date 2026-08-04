package com.kuronami.welcomeboard.config;

/**
 * The one player-facing display choice (DESIGN_COMPILE.md U6): which of the three candidate
 * screen layouts {@link com.kuronami.welcomeboard.client.WelcomeBoardScreen} should draw.
 *
 * <p>No Minecraft/NeoForge types here on purpose, same reason as {@code
 * com.kuronami.steadysight.compute.StrengthPreset} in the mod-061 playbook this mod follows: a
 * {@code ModConfigSpec.EnumValue} needs an actual enum that implements {@code TranslatableEnum} to
 * show translated button labels, and that interface pulls in NeoForge/Minecraft types that would
 * make this enum unloadable from the test source set the moment a test touches one of its
 * constants. The NeoForge-facing wrapper lives in {@code neoforge/.../config/LayoutPresetOption}.
 */
public enum LayoutPreset {
    /** Centered modal card — the safe, dialog-like default. */
    CARD,
    /** Thin horizontal strip near the top of the screen — least intrusive. */
    BANNER,
    /** Large, mostly-fullscreen takeover — most attention-grabbing. */
    FULL;

    public static final LayoutPreset DEFAULT = CARD;
}
