package com.kuronami.welcomeboard.config;

import com.kuronami.welcomeboard.Constants;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

import java.util.Locale;

/**
 * The NeoForge-facing wrapper around {@link LayoutPreset} — a {@code ModConfigSpec.EnumValue}
 * needs an actual enum to store, and that enum needs to implement {@link TranslatableEnum} for
 * {@code ConfigurationScreen} to show translated button labels instead of raw constant names.
 *
 * <p>Same split as mod-061's {@code config.Strength}/{@code compute.StrengthPreset}, for the same
 * reason (see {@link LayoutPreset}'s Javadoc): the test source set has no NeoForge/Minecraft
 * classes on its classpath, so a {@link TranslatableEnum} implementor cannot even be loaded from a
 * test. This class has no logic of its own beyond delegating to {@link LayoutPreset}.
 */
public enum LayoutPresetOption implements TranslatableEnum {
    CARD(LayoutPreset.CARD),
    BANNER(LayoutPreset.BANNER),
    FULL(LayoutPreset.FULL);

    private final LayoutPreset preset;

    LayoutPresetOption(LayoutPreset preset) {
        this.preset = preset;
    }

    public LayoutPreset toPreset() {
        return preset;
    }

    @Override
    public Component getTranslatedName() {
        return Component.translatable(Constants.MOD_ID + ".configuration.layoutPreset." + name().toLowerCase(Locale.ROOT));
    }
}
