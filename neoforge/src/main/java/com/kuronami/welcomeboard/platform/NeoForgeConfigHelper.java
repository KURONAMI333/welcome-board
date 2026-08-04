package com.kuronami.welcomeboard.platform;

import com.kuronami.welcomeboard.Constants;
import com.kuronami.welcomeboard.config.LayoutPreset;
import com.kuronami.welcomeboard.config.LayoutPresetOption;
import com.kuronami.welcomeboard.platform.services.IConfigHelper;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge implementation: backs {@link IConfigHelper} with a real {@code ModConfigSpec}, so
 * players get NeoForge's built-in {@code ConfigurationScreen} for free (registered from the main
 * mod class's constructor — see {@code WelcomeBoard}). Two fields only, both non-numeric
 * (DESIGN_COMPILE.md U6 / this project's standing rule against raw decimals in config screens).
 */
public final class NeoForgeConfigHelper implements IConfigHelper {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .translation(Constants.MOD_ID + ".configuration.enabled")
            .comment("Whether the welcome board can be shown at all.")
            .define("enabled", true);

    public static final ModConfigSpec.EnumValue<LayoutPresetOption> LAYOUT_PRESET = BUILDER
            .translation(Constants.MOD_ID + ".configuration.layoutPreset")
            .comment("Which screen layout the welcome board uses.")
            .defineEnum("layoutPreset", LayoutPresetOption.CARD);

    public static final ModConfigSpec SPEC = BUILDER.build();

    @Override
    public boolean enabled() {
        return ENABLED.get();
    }

    @Override
    public LayoutPreset layoutPreset() {
        return LAYOUT_PRESET.get().toPreset();
    }
}
