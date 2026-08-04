package com.kuronami.welcomeboard.platform.services;

/**
 * Loader abstraction for the player-facing config (DESIGN_COMPILE.md U6). NeoForge backs this
 * with a real {@code ModConfigSpec} and gets a free in-game screen via {@code
 * registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new)}; Fabric has no
 * built-in config-screen mechanism, so it returns fixed defaults (same convention already used by
 * this project's other multiloader mods, e.g. mod-051's {@code IConfigHelper}).
 */
public interface IConfigHelper {

    /** Whether the welcome board should ever be shown at all. Defaults to {@code true}. */
    boolean enabled();
}
